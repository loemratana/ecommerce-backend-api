package com.example.ecommerce_api.product.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Size(max = 500, message = "Image cover must not exceed 500 characters")
    private String imageCover;

    @NotNull(message = "Sub category is required")
    private Long subCategoryId;

    private Long brandId;

    @Valid
    @NotEmpty(message = "At least one product variant is required")
    private List<ProductVariantsRequest> variants = new ArrayList<>();

    @Valid
    private List<ProductImageRequest> images = new ArrayList<>();
}
