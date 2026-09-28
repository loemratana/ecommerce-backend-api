package com.example.ecommerce_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignPermissionsRequest {

    @NotNull(message = "permissionIds is required")
    @ArraySchema(schema = @Schema(example = "1"))
    private Set<Long> permissionIds;
}
