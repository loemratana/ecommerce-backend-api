package com.example.ecommerce_api.permission.service.impl;

import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.permission.dto.request.PermissionRequest;
import com.example.ecommerce_api.permission.dto.response.PermissionResponse;
import com.example.ecommerce_api.permission.entity.Permission;
import com.example.ecommerce_api.permission.mapper.PermissionMapper;
import com.example.ecommerce_api.permission.repository.PermissionRepository;
import com.example.ecommerce_api.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public PermissionResponse createPermission(PermissionRequest request) {

        if (permissionRepository.existsByNameIgnoreCase(request.getName())) {
            log.warn("Attempted to create permission with duplicate name '{}'", request.getName());
            throw new BusinessException("Permission name already exists");
        }

        Permission permission = permissionMapper.toEntity(request);
        Permission saved = permissionRepository.save(permission);

        log.info("Created permission '{}' with id {}", saved.getName(), saved.getId());
        return permissionMapper.toResponse(saved);
    }

    @Override
    public PermissionResponse updatePermission(Long id, PermissionRequest request) {

        Permission permission = findPermissionById(id);

        if (request.getName() != null
                && !request.getName().equalsIgnoreCase(permission.getName())
                && permissionRepository.existsByNameIgnoreCase(request.getName())) {
            log.warn("Attempted to rename permission {} to duplicate name '{}'", id, request.getName());
            throw new BusinessException("Permission name already exists");
        }

        permissionMapper.updateEntity(permission, request);
        Permission saved = permissionRepository.save(permission);

        log.info("Updated permission {}", id);
        return permissionMapper.toResponse(saved);
    }

    @Override
    public void deletePermission(Long id) {

        findPermissionById(id);

        if (permissionRepository.existsRoleWithPermission(id)) {
            log.warn("Attempted to delete permission {} while still assigned to roles", id);
            throw new BusinessException("Cannot delete permission because it is assigned to roles");
        }

        permissionRepository.deleteById(id);
        log.info("Deleted permission {}", id);
    }

    @Override
    public List<PermissionResponse> findAll() {
        return permissionMapper.toResponseList(permissionRepository.findAll());
    }

    @Override
    public PermissionResponse findById(Long id) {
        return permissionMapper.toResponse(findPermissionById(id));
    }

    private Permission findPermissionById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
    }
}
