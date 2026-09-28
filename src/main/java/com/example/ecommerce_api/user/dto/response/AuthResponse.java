package com.example.ecommerce_api.user.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresInSeconds;

    @Override
    public String toString() {
        return "AuthResponse{tokenType=" + tokenType + ", expiresInSeconds=" + expiresInSeconds + ", accessToken=[PROTECTED], refreshToken=[PROTECTED]}";
    }
}
