package com.example.ecommerce_api.user.dto.request;


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
public class RoleRequest {

    @NotBlank(message = "roles name is required")
    @Schema(example = "ADMIN")
    private String roleName;


    @Schema(example = "true")
    private boolean active;


    @Size(min = 2 ,max =100 , message = "Description name must be between 2 and 100 characters")
    @Schema(example = "Admin is role that have all permission in system")
    private String Description;

}
