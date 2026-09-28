package com.example.ecommerce_api.category.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubCategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    @Schema(example = "Phones")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must be at most 255 characters")
    @Schema(example = "phones")
    private String slug;

    @NotNull(message = "Category id is required")
    @Schema(example = "1")
    private Long categoryId;
}
