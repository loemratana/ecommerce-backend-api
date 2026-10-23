package com.example.ecommerce_api.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the admin customer list. */
@Builder
@Getter
public class CustomerSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String profileImage;
    private boolean active;
    private LocalDateTime joinedAt;

    /** All orders, whatever their status. */
    private long orderCount;
    /** Sum of paid, non-cancelled orders. */
    private BigDecimal totalSpent;
    private LocalDateTime lastOrderDate;
}
