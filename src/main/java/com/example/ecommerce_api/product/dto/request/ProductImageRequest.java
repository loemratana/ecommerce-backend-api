package com.example.ecommerce_api.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageRequest {

    private Long id;

    @NotBlank(message = "Image URL is required")
    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    private String imageUrl;

    /** Must match one of the product's variant colors; omit for an image shown for every color. */
    @Size(max = 50, message = "Image color must not exceed 50 characters")
    private String color;

    private Integer sortOrder;
}
