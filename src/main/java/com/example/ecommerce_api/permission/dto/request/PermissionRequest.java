package com.example.ecommerce_api.permission.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionRequest {

    @NotBlank(message = "Permission name is required")
    @Size(max = 100)
    @Schema(example = "PRODUCT_CREATE")
    private String name;

    @NotBlank(message = "Resource is required")
    @Size(max = 50)
    @Schema(example = "PRODUCT")
    private String resource;

    @NotBlank(message = "Action is required")
    @Size(max = 50)
    @Schema(example = "CREATE")
    private String action;

    @Size(max = 255)
    @Schema(example = "Allows creating new products")
    private String description;
}
