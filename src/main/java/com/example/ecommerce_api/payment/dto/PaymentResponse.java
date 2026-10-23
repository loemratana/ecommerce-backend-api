package com.example.ecommerce_api.payment.dto;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private PaymentMethod method;
    private PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String currency;
    /** Send the customer here to pay; null for cash on delivery. */
    private String redirectUrl;
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
