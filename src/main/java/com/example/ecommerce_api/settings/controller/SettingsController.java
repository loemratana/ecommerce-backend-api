package com.example.ecommerce_api.settings.controller;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.settings.dto.*;
import com.example.ecommerce_api.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/settings")
@Tag(name = "settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @Operation(summary = "Get all store settings")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.get()));
    }

    @Operation(summary = "Store name, contact details and currency for the storefront", description = "No login needed")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PublicSettingsResponse>> getPublic() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getPublic()));
    }

    @Operation(summary = "Update store details", description = "Also printed on invoices")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/store")
    public ResponseEntity<ApiResponse<SettingsResponse>> updateStore(@Valid @RequestBody StoreSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateStore(request), "Store settings saved"));
    }

    @Operation(summary = "Update tax, shipping, unpaid-order timeout and return window",
            description = "Applies to orders placed from now on; taxRate is a fraction (0.10 = 10%)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/orders")
    public ResponseEntity<ApiResponse<SettingsResponse>> updateOrders(@Valid @RequestBody OrderSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateOrders(request), "Order settings saved"));
    }

    @Operation(summary = "Update currency and timezone", description = "Currency is a label only; amounts are not converted")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/localization")
    public ResponseEntity<ApiResponse<SettingsResponse>> updateLocalization(@Valid @RequestBody LocalizationSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateLocalization(request), "Localization settings saved"));
    }

    @Operation(summary = "Update the login session length", description = "Applies to tokens issued from now on")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/security")
    public ResponseEntity<ApiResponse<SettingsResponse>> updateSecurity(@Valid @RequestBody SecuritySettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateSecurity(request), "Security settings saved"));
    }
}
