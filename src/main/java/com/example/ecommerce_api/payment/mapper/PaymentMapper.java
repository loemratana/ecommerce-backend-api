package com.example.ecommerce_api.payment.mapper;

import com.example.ecommerce_api.payment.dto.PaymentResponse;
import com.example.ecommerce_api.payment.entity.Payment;
import com.example.ecommerce_api.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        User customer = payment.getOrder().getUser();
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .customerName(customer != null ? (customer.getFirstName() + " " + customer.getLastName()).trim() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .refundedAmount(payment.getRefundedAmount())
                .currency(payment.getCurrency())
                .redirectUrl(payment.getRedirectUrl())
                .failureReason(payment.getFailureReason())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
