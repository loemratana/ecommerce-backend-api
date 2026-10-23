package com.example.ecommerce_api.order.dto;


import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.Enum.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class OrderResponse {

    private Long id;
    private String orderNumber;

    private OrderStatus status;
    /** Null until the customer starts paying. */
    private PaymentMethod paymentMethod;
    private OrderCustomerResponse customer;

    private ShippingAddressResponse shippingAddress;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxPrice;
    private BigDecimal shippingPrice;
    private BigDecimal totalOrderPrice;
    private BigDecimal refundedAmount;
    private LocalDateTime deliveredAt;

    private List<OrderItemResponse> items;

    private List<String> couponCodes;
    private List<OrderCouponResponse> coupons;
    private LocalDateTime orderDate;
    private LocalDateTime updateDate;
}
