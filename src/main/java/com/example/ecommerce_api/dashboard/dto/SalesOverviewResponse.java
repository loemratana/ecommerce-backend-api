package com.example.ecommerce_api.dashboard.dto;

import com.example.ecommerce_api.Enum.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Builder
@Getter
public class SalesOverviewResponse {

    private int year;
    /** Always 12 buckets, Jan..Dec of {@code year}, with 0 for months without sales. */
    private List<SalesBucketResponse> months;
    /** The current calendar month, whatever {@code year} is. */
    private SalesBucketResponse thisMonth;
    private SalesBucketResponse lastMonth;
    private BigDecimal todayRevenue;
    private BigDecimal yearRevenue;
    /** All orders, every status, all time. */
    private long totalOrders;
    /** Every status is present, with 0 when no order has it. */
    private Map<OrderStatus, Long> statusCounts;
}
