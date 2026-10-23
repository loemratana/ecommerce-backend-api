package com.example.ecommerce_api.returns.repository;

import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.returns.entity.ReturnRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    @EntityGraph(attributePaths = {"order", "user"})
    Optional<ReturnRequest> findByReturnNumber(String returnNumber);

    @EntityGraph(attributePaths = {"order", "user"})
    Optional<ReturnRequest> findByReturnNumberAndUserId(String returnNumber, Long userId);

    /** For status changes, so two admins (or admin + customer) can't race on one return. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReturnRequest r WHERE r.returnNumber = :returnNumber")
    Optional<ReturnRequest> lockByReturnNumber(@Param("returnNumber") String returnNumber);

    @EntityGraph(attributePaths = "order")
    Page<ReturnRequest> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "user"})
    List<ReturnRequest> findByOrder_OrderNumberOrderByCreatedAtDesc(String orderNumber);

    @EntityGraph(attributePaths = "order")
    List<ReturnRequest> findByOrder_OrderNumberAndUserIdOrderByCreatedAtDesc(String orderNumber, Long userId);

    /** {@code search} must already be lower-cased and wrapped in %...%. */
    @EntityGraph(attributePaths = {"order", "user"})
    @Query(value = """
            SELECT r FROM ReturnRequest r JOIN r.order o JOIN r.user u
            WHERE (:status IS NULL OR r.status = :status)
              AND (:search = '%%'
                   OR LOWER(r.returnNumber) LIKE :search
                   OR LOWER(o.orderNumber) LIKE :search
                   OR LOWER(u.email) LIKE :search
                   OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE :search)
            """,
            countQuery = """
            SELECT COUNT(r) FROM ReturnRequest r JOIN r.order o JOIN r.user u
            WHERE (:status IS NULL OR r.status = :status)
              AND (:search = '%%'
                   OR LOWER(r.returnNumber) LIKE :search
                   OR LOWER(o.orderNumber) LIKE :search
                   OR LOWER(u.email) LIKE :search
                   OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE :search)
            """)
    Page<ReturnRequest> search(@Param("status") ReturnStatus status, @Param("search") String search, Pageable pageable);

    /** Rows of [ReturnStatus, Long]. */
    @Query("SELECT r.status, COUNT(r) FROM ReturnRequest r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();
}
