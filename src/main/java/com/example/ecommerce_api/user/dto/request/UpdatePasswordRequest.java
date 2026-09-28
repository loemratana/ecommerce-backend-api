package com.example.ecommerce_api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePasswordRequest {


    @NotBlank
    @Schema(example = "OldP@ssw0rd123")
    private String oldPassword;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(example = "NewP@ssw0rd123")
    private String newPassword;

    @NotBlank(message = "Confirm Password is required")
    @Schema(example = "NewP@ssw0rd123")
    private String confirmNewPassword;
}
