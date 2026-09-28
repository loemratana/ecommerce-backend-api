package com.example.ecommerce_api.permission.controller;

import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.permission.dto.request.PermissionRequest;
import com.example.ecommerce_api.permission.dto.response.PermissionResponse;
import com.example.ecommerce_api.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(description = "Create a new permission")
    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(@Valid @RequestBody PermissionRequest request) {

        PermissionResponse response = permissionService.createPermission(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Permission created successfully"));
    }

    @Operation(description = "Update an existing permission")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(@PathVariable Long id,
                                                                              @Valid @RequestBody PermissionRequest request) {

        PermissionResponse response = permissionService.updatePermission(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Permission updated successfully"));
    }

    @Operation(description = "Get a permission by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermissionById(@PathVariable Long id) {

        PermissionResponse response = permissionService.findById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(description = "Get all permissions")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {

        List<PermissionResponse> response = permissionService.findAll();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(description = "Delete a permission by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {

        permissionService.deletePermission(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Permission deleted successfully"));
    }
}
