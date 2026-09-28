package com.example.ecommerce_api.user.controller;


import com.example.ecommerce_api.common.response.ApiResponse;
import com.example.ecommerce_api.user.dto.request.AssignPermissionsRequest;
import com.example.ecommerce_api.user.dto.request.RoleRequest;
import com.example.ecommerce_api.user.dto.response.RoleResponse;
import com.example.ecommerce_api.user.services.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(description = "Create a new role")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequest request) {

        RoleResponse response = roleService.createRole(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Role created successfully"));
    }

    @Operation(description = "Update an existing role")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(@PathVariable Long id,
                                                                 @Valid @RequestBody RoleRequest request) {

        RoleResponse response = roleService.updateRole(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Role updated successfully"));
    }

    @Operation(description = "Get a role by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {

        RoleResponse response = roleService.findById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(description = "Get all roles")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {

        List<RoleResponse> response = roleService.findAll();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(description = "Delete a role by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {

        roleService.deleteRole(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }

    @Operation(description = "Replace the set of permissions assigned to a role")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(@PathVariable Long id,
                                                                         @Valid @RequestBody AssignPermissionsRequest request) {

        RoleResponse response = roleService.assignPermissions(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Permissions assigned successfully"));
    }
}
