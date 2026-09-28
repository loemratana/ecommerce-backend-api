package com.example.ecommerce_api.user.controller;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.security.service.UserDetailsImpl;
import com.example.ecommerce_api.user.dto.request.AddressRequest;
import com.example.ecommerce_api.user.dto.response.AddressResponse;
import com.example.ecommerce_api.user.services.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(description = "List all addresses for the authenticated user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        List<AddressResponse> response = addressService.getAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(description = "Add a new address for the authenticated user")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody AddressRequest request) {

        AddressResponse response = addressService.addAddress(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Address added successfully"));
    }

    @Operation(description = "Get one address belonging to the authenticated user")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Long id) {

        AddressResponse response = addressService.getAddress(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(description = "Update an address belonging to the authenticated user")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {

        AddressResponse response = addressService.updateAddress(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Address updated successfully"));
    }

    @Operation(description = "Delete an address belonging to the authenticated user")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Long id) {

        addressService.deleteAddress(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Address deleted successfully"));
    }

    @Operation(description = "Set an address as the default, clearing the default flag on all others")
    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Long id) {

        AddressResponse response = addressService.setDefaultAddress(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response, "Default address updated"));
    }
}
