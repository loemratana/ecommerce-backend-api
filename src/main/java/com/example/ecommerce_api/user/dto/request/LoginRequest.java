package com.example.ecommerce_api.user.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(example = "john.doe@example.com")
    private String email;

    @NotBlank
    @Schema(example = "P@ssw0rd123")
    private String password;

    @NotBlank
    private String deviceId;

    private String deviceName;

    private String deviceType;

    @Override
    public String toString() {
        return "LoginRequest{email=" + email + ", deviceId=" + deviceId + ", password=[PROTECTED]}";
    }
}
