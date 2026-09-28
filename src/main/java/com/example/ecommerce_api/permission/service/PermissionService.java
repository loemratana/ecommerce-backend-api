package com.example.ecommerce_api.permission.service;

import com.example.ecommerce_api.permission.dto.request.PermissionRequest;
import com.example.ecommerce_api.permission.dto.response.PermissionResponse;

import java.util.List;

public interface PermissionService {

    PermissionResponse createPermission(PermissionRequest request);

    PermissionResponse updatePermission(Long id, PermissionRequest request);

    void deletePermission(Long id);

    List<PermissionResponse> findAll();

    PermissionResponse findById(Long id);
}
