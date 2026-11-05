package com.example.ecommerce_api.product.dto.request;


import com.example.ecommerce_api.Enum.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {


    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must not exceed 255 characters")
    private String slug;

    private String description;

    @Size(max = 2048, message = "Image cover must not exceed 2048 characters")
    private String imageCover;

    @NotNull(message = "Sub category is required")
    private Long subCategoryId;

    private Long brandId;

    /** MEN, WOMEN or UNISEX; optional. */
    private ProductType productType;

    /** Product-wide discount (0-100); used by variants that have no discount of their own. */
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount percent cannot be negative")
    @DecimalMax(value = "100.0", inclusive = true, message = "Discount percent cannot exceed 100")
    private BigDecimal discountPercent;

    @Valid
    @NotEmpty(message = "At least one product variant is required")
    private List<ProductVariantsRequest> variants = new ArrayList<>();

    @Valid
    private List<ProductImageRequest> images = new ArrayList<>();
}
