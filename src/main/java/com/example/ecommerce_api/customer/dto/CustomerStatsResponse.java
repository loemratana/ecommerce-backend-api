package com.example.ecommerce_api.customer.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CustomerStatsResponse {

    private long totalCustomers;
    private long activeCustomers;
    private long inactiveCustomers;
    private long newThisMonth;
    /** Customers who have placed at least one order. */
    private long customersWithOrders;
}
