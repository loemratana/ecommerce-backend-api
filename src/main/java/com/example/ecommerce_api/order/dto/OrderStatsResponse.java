package com.example.ecommerce_api.order.dto;

import com.example.ecommerce_api.Enum.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Builder
@Getter
public class OrderStatsResponse {

    private long totalOrders;
    /** Every status is present, with 0 when no order has it. */
    private Map<OrderStatus, Long> byStatus;
    /** Sum of orders that were paid and not cancelled. */
    private BigDecimal totalRevenue;
}
