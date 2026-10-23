package com.example.ecommerce_api.payment.dto;

import com.example.ecommerce_api.Enum.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitiatePaymentRequest {

    @NotBlank(message = "Order number is required")
    @Schema(example = "ORD-20261023-3F9A1C7B")
    private String orderNumber;

    @NotNull(message = "Payment method is required")
    @Schema(example = "CASH_ON_DELIVERY")
    private PaymentMethod method;
}
