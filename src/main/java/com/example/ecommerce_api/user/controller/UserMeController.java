package com.example.ecommerce_api.user.controller;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.security.service.UserDetailsImpl;
import com.example.ecommerce_api.user.dto.request.UpdatePasswordRequest;
import com.example.ecommerce_api.user.dto.request.UpdateProfileRequest;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import com.example.ecommerce_api.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserMeController {

    private final UserService userService;

    @Operation(description = "Get the authenticated user's profile")
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        UserResponse response = userService.findUserById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(description = "Update the authenticated user's name, phone, and profile image")
    @PatchMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse response = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    @Operation(description = "Change the authenticated user's password")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<UserResponse>> updatePassword(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody UpdatePasswordRequest request) {

        UserResponse response = userService.updatePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Password updated successfully"));
    }

    @Operation(description = "Soft-deactivate the authenticated user's account")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@AuthenticationPrincipal UserDetailsImpl principal) {
        userService.deactivateUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated successfully"));
    }
}
