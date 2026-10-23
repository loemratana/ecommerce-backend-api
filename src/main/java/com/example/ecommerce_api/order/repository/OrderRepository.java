package com.example.ecommerce_api.order.repository;

import java.time.LocalDateTime;
import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository  extends JpaRepository<Order,Long> {


    @EntityGraph(attributePaths = {"orderItems", "user"})
    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = "orderItems")
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    /** For status changes; the lock keeps two admins (or admin + customer) from racing on one order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> lockByOrderNumber(@Param("orderNumber") String orderNumber);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * A customer's delivered orders still inside the return window, most recently delivered first.
     * A null deliveredAt (delivered before the date was recorded) has no deadline, matching the
     * return eligibility check.
     */
    @EntityGraph(attributePaths = "orderItems")
    @Query("""
            SELECT o FROM Order o
            WHERE o.user.id = :userId AND o.status = :status
              AND (o.deliveredAt IS NULL OR o.deliveredAt >= :deliveredSince)
            ORDER BY o.deliveredAt DESC NULLS LAST, o.orderDate DESC
            """)
    List<Order> findReturnCandidates(@Param("userId") Long userId,
                                     @Param("status") OrderStatus status,
                                     @Param("deliveredSince") LocalDateTime deliveredSince);

    /** Unpaid orders placed before the cutoff; they are holding reserved stock. */
    @Query("SELECT o.orderNumber FROM Order o WHERE o.status = :status AND o.orderDate < :cutoff " +
            "AND (o.paymentMethod IS NULL OR o.paymentMethod <> com.example.ecommerce_api.Enum.PaymentMethod.CASH_ON_DELIVERY) " +
            "ORDER BY o.orderDate")
    List<String> findOrderNumbersByStatusPlacedBefore(@Param("status") OrderStatus status,
                                                      @Param("cutoff") LocalDateTime cutoff,
                                                      Pageable pageable);

    // ----- admin list: the customer is shown on every row, so fetch it with the order -----

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** Orders placed in [from, to) for the Excel export; a null status means every status. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT o FROM Order o
            WHERE o.orderDate >= :from AND o.orderDate < :to
              AND (:status IS NULL OR o.status = :status)
            """)
    Slice<Order> findForExport(@Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("status") OrderStatus status,
                               Pageable pageable);


    /** Rows of [month (Integer 1-12), orderCount (Long), revenue (BigDecimal)] for orders placed in [from, to). */
    @Query("""
select  extract(MONTH FROM o.orderDate) , count(o),coalesce(sum(o.totalOrderPrice - o.refundedAmount),0) 
from Order o 
where o.status IN :statuses AND o.orderDate >=: from AND o.orderDate< :to
group by extract(MONTH from o.orderDate) 
""")
    List<Object[]> sumGroupedByMonth(@Param("statuses") Collection<OrderStatus> statuses
            ,@Param("from") LocalDateTime from,@Param("to") LocalDateTime to);


    /** A single row of [orderCount (Long), revenue (BigDecimal)] for orders placed in [from, to). */
    @Query("""
            SELECT COUNT(o), COALESCE(SUM(o.totalOrderPrice - o.refundedAmount), 0)
            FROM Order o
            WHERE o.status IN :statuses AND o.orderDate >= :from AND o.orderDate < :to
            """)
    List<Object[]> sumBetween(@Param("statuses") Collection<OrderStatus> statuses,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    /** {@code search} must already be lower-cased and wrapped in %...%. */
    @EntityGraph(attributePaths = "user")
    @Query(value = """
            SELECT o FROM Order o JOIN o.user u
            WHERE (:status IS NULL OR o.status = :status)
              AND (LOWER(o.orderNumber) LIKE :search
                   OR LOWER(u.email) LIKE :search
                   OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE :search)
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o JOIN o.user u
            WHERE (:status IS NULL OR o.status = :status)
              AND (LOWER(o.orderNumber) LIKE :search
                   OR LOWER(u.email) LIKE :search
                   OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE :search)
            """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("search") String search, Pageable pageable);

    // ----- stats -----

    /** Rows of [OrderStatus, Long]. */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countGroupedByStatus();

    /** Net revenue: order totals minus refunds from returns. */
    @Query("SELECT COALESCE(SUM(o.totalOrderPrice - o.refundedAmount), 0) FROM Order o WHERE o.status IN :statuses")
    BigDecimal sumTotalByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    // ----- per-customer figures -----

    /** Rows of [userId (Long), orderCount (Long), lastOrderDate (LocalDateTime)], all statuses. */
    @Query("SELECT o.user.id, COUNT(o), MAX(o.orderDate) FROM Order o WHERE o.user.id IN :userIds GROUP BY o.user.id")
    List<Object[]> countAndLastOrderByUserIds(@Param("userIds") Collection<Long> userIds);

    /** Rows of [userId (Long), spent (BigDecimal)], counting only the given (paid) statuses. */
    @Query("SELECT o.user.id, SUM(o.totalOrderPrice - o.refundedAmount) FROM Order o WHERE o.user.id IN :userIds AND o.status IN :statuses GROUP BY o.user.id")
    List<Object[]> sumTotalByUserIds(@Param("userIds") Collection<Long> userIds, @Param("statuses") Collection<OrderStatus> statuses);

    /** Rows of [OrderStatus, Long] for one customer. */
    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.user.id = :userId GROUP BY o.status")
    List<Object[]> countGroupedByStatusForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o")
    long countDistinctCustomers();
}
