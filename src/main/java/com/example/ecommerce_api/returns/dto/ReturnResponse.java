package com.example.ecommerce_api.returns.dto;

import com.example.ecommerce_api.Enum.ReturnStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ReturnResponse {
    private String returnNumber;
    private ReturnStatus status;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private String reason;
    private String adminNote;
    /** What the customer gets back if every item is received (coupons and tax shared out, shipping excluded). */
    private BigDecimal estimatedRefund;
    /** Actually refunded; set once RECEIVED. */
    private BigDecimal refundAmount;
    private List<ReturnItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime receivedAt;
}
