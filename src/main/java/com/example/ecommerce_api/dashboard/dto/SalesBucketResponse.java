package com.example.ecommerce_api.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;


@Builder
@Getter
public class SalesBucketResponse {
    /** "Jan", "Feb", ... */
    private String label;
    /** Paid orders only. */
    private long orders;
    /** Net revenue: order totals minus refunds. */
    private BigDecimal revenue;
}
