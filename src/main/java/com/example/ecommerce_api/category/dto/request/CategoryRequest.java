package com.example.ecommerce_api.category.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    @Schema(example = "Electronics")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must be at most 255 characters")
    @Schema(example = "electronics")
    private String slug;

    @Size(max = 500, message = "Image must be at most 500 characters")
    @Schema(example = "https://example.com/images/electronics.jpg")
    private String image;
}
