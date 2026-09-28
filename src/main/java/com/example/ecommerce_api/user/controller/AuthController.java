package com.example.ecommerce_api.user.controller;


import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.security.service.UserDetailsImpl;
import com.example.ecommerce_api.user.dto.request.LoginRequest;
import com.example.ecommerce_api.user.dto.request.RefreshTokenRequest;
import com.example.ecommerce_api.user.dto.request.RegisterRequest;
import com.example.ecommerce_api.user.dto.response.AuthResponse;
import com.example.ecommerce_api.user.dto.response.DeviceResponse;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import com.example.ecommerce_api.user.services.AuthService;
import com.example.ecommerce_api.user.services.UserDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserDeviceService userDeviceService;

    @Operation(description = "Authenticate with email/password and register or refresh the calling device")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @Operation(description = "Register a new user with email and password")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userResponse, "Register successful"));
    }

    @Operation(description = "Exchange a valid refresh token for a new access/refresh token pair")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @Operation(description = "Revoke the session identified by this refresh token (current device only)")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
    }

    @Operation(description = "Revoke every active session for the authenticated user, across all devices")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal UserDetailsImpl principal) {
        authService.logoutAll(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out from all devices"));
    }

    @Operation(description = "List the authenticated user's known devices and their session status")
    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> listDevices(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestParam(required = false) Long currentDeviceId) {

        List<DeviceResponse> devices = userDeviceService.listDevices(principal.getId(), currentDeviceId);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @Operation(description = "Revoke a single device's session(s), owned by the authenticated user")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> revokeDevice(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Long deviceId) {

        userDeviceService.revokeDevice(principal.getId(), deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device revoked"));
    }
}
