package com.example.ecommerce_api.order.dto;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.Enum.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
public class OrderSummaryResponse {

    private String orderNumber;
    private OrderStatus status;
    /** Null until the customer starts paying. */
    private PaymentMethod paymentMethod;
    private BigDecimal totalOrderPrice;
    private String customerName;
    private String customerEmail;
    private int itemCount;
    private LocalDateTime orderDate;
}
