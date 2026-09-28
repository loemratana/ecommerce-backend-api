package com.example.ecommerce_api.user.services;

import com.example.ecommerce_api.user.dto.request.LoginRequest;
import com.example.ecommerce_api.user.dto.request.RegisterRequest;
import com.example.ecommerce_api.user.dto.response.AuthResponse;
import com.example.ecommerce_api.user.dto.response.UserResponse;

/**
 * Thin orchestrator over authentication and password credentials, session
 * (device + refresh token) issuance, and access-token minting. Session
 * lifecycle itself lives in RefreshTokenService / UserDeviceService.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String rawRefreshToken);

    UserResponse register(RegisterRequest registerRequest);

    void logout(String rawRefreshToken);

    void logoutAll(Long userId);
}
