package com.example.ecommerce_api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shared body for /refresh and /logout - both operate on "the session this
 * refresh token identifies".
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;

    @Override
    public String toString() {
        return "RefreshTokenRequest{refreshToken=[PROTECTED]}";
    }
}
