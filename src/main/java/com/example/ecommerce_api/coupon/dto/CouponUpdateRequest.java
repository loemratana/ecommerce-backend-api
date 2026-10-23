package com.example.ecommerce_api.coupon.dto;

import com.example.ecommerce_api.Enum.DiscountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponUpdateRequest {
    @NotBlank(message = "Coupon code is required")
    @Size(max = 50)
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

    @NotNull
    @Future
    private LocalDateTime expiryDate;

    @NotNull
    private Boolean active;


    @Positive(message = "Usage limit must be greater than 0")
    private Integer usageLimit;

    @AssertTrue(message = "Percentage discount cannot exceed 100%")
    private boolean isPercentageWithinLimit() {
        return discountType != DiscountType.PERCENTAGE
                || discountValue == null
                || discountValue.compareTo(BigDecimal.valueOf(100)) <= 0;
    }
}
