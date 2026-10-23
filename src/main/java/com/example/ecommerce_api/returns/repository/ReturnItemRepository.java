package com.example.ecommerce_api.returns.repository;

import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.returns.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

    /** Rows of [orderItemId (Long), quantity already in returns with the given statuses (Long)]. */
    @Query("""
            SELECT i.orderItem.id, SUM(i.quantity) FROM ReturnItem i
            WHERE i.orderItem.order.id = :orderId AND i.returnRequest.status IN :statuses
            GROUP BY i.orderItem.id
            """)
    List<Object[]> sumQuantityByOrderItem(@Param("orderId") Long orderId,
                                          @Param("statuses") Collection<ReturnStatus> statuses);

    /** Same as {@link #sumQuantityByOrderItem}, for several orders in one query. */
    @Query("""
            SELECT i.orderItem.id, SUM(i.quantity) FROM ReturnItem i
            WHERE i.orderItem.order.id IN :orderIds AND i.returnRequest.status IN :statuses
            GROUP BY i.orderItem.id
            """)
    List<Object[]> sumQuantityByOrderItemForOrders(@Param("orderIds") Collection<Long> orderIds,
                                                   @Param("statuses") Collection<ReturnStatus> statuses);
}
