package com.example.ecommerce_api.settings.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class OrderSettingsRequest {

    /** Fraction, not percent: 0.10 = 10%. */
    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.0000", message = "Tax rate cannot be negative")
    @DecimalMax(value = "1.0000", message = "Tax rate cannot be more than 100%")
    @Digits(integer = 1, fraction = 4)
    private BigDecimal taxRate;

    @NotNull(message = "Shipping fee is required")
    @DecimalMin(value = "0.00", message = "Shipping fee cannot be negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal shippingFee;

    @NotNull(message = "Pending timeout is required")
    @Min(value = 5, message = "Pending timeout must be at least 5 minutes")
    @Max(value = 10080, message = "Pending timeout cannot be more than 7 days")
    private Integer pendingTimeoutMinutes;

    @NotNull(message = "Return window is required")
    @Min(value = 0, message = "Return window cannot be negative")
    @Max(value = 365, message = "Return window cannot be more than 365 days")
    private Integer returnWindowDays;
}
