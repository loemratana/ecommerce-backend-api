package com.example.ecommerce_api.review.dto;


import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.user.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {


    @Size(max = 255)
    private String title;


    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("5.0")
    @Digits(integer = 1, fraction = 1)
    private BigDecimal ratings;

    @Size(max = 5000)
    private String comment;
}
