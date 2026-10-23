package com.example.ecommerce_api.coupon.dto;


import com.example.ecommerce_api.Enum.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponCreateRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal discountValue;

    /** Order subtotal required to use the coupon; null = no minimum */
    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal minOrderAmount;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;

    private Boolean active = true;

    /**
     * Maximum total number of times this coupon can be used.
     * null = unlimited
     */
    @Positive(message = "Usage limit must be greater than 0")
    private Integer usageLimit;

    @AssertTrue(message = "Percentage discount cannot exceed 100%")
    private boolean isPercentageWithinLimit() {
        return discountType != DiscountType.PERCENTAGE
                || discountValue == null
                || discountValue.compareTo(BigDecimal.valueOf(100)) <= 0;
    }
}
