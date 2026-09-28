package com.example.ecommerce_api.user.services.impl;


import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.permission.entity.Permission;
import com.example.ecommerce_api.permission.repository.PermissionRepository;
import com.example.ecommerce_api.user.dto.request.AssignPermissionsRequest;
import com.example.ecommerce_api.user.dto.request.RoleRequest;
import com.example.ecommerce_api.user.dto.response.RoleResponse;
import com.example.ecommerce_api.user.entity.Role;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.mapper.RoleMapper;
import com.example.ecommerce_api.user.repository.RoleRepository;
import com.example.ecommerce_api.user.services.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleServiceImpl  implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private final RoleMapper roleMapper;

    @Override
    public RoleResponse createRole(RoleRequest roleRequest) {

        if (roleRepository.existsByNameIgnoreCase(roleRequest.getRoleName()))
        {
            log.warn("Attempted to create role with duplicate name '{}'", roleRequest.getRoleName());
            throw new BusinessException("Name already exists");

        }

        Role role = roleMapper.toEntity(roleRequest);

        Role savedRole  = roleRepository.save(role);

        log.info("Created role '{}' with id {}", savedRole.getName(), savedRole.getId());

        return roleMapper.toResponse(savedRole);
    }

    @Override
    public RoleResponse updateRole(Long id, RoleRequest roleRequest) {

        Role role = findRoleById(id);

        if (roleRequest.getRoleName() != null && !roleRequest.getRoleName().equalsIgnoreCase(role.getName())
                && roleRepository.existsByNameIgnoreCase(roleRequest.getRoleName())
        )
        {
            log.warn("Attempted to rename role {} to duplicate name '{}'", id, roleRequest.getRoleName());
            throw new BusinessException("Role name already exists");
        }

        roleMapper.updateEntity(role, roleRequest);

        Role save  =  roleRepository.save(role);

        log.info("Updated role {}", id);

        return roleMapper.toResponse(save);
    }

    @Override
    public void deleteRole(Long id) {

        Role role =findRoleById(id);
        if (roleRepository.existsUserWithRole(id)) {
            log.warn("Attempted to delete role {} while still assigned to users", id);
            throw new BusinessException("Cannot delete role because it is assigned to users");
        }

        roleRepository.deleteById(id);
        log.info("Deleted role {}", id);
    }

    @Override
    public List<RoleResponse> findAll() {

        List<Role> roles = roleRepository.findAll();
        return   roleMapper.toResponseList(roles);
    }

    @Override
    public RoleResponse findById(Long id) {
        return roleMapper.toResponse(findRoleById(id));
    }

    @Override
    public RoleResponse assignPermissions(Long id, AssignPermissionsRequest request) {

        Role role = findRoleById(id);

        List<Permission> permissions = permissionRepository.findAllByIdIn(request.getPermissionIds());

        if (permissions.size() != request.getPermissionIds().size()) {
            Set<Long> foundIds = permissions.stream().map(Permission::getId).collect(Collectors.toSet());
            List<Long> missingIds = request.getPermissionIds().stream()
                    .filter(permissionId -> !foundIds.contains(permissionId))
                    .toList();
            log.warn("Attempted to assign unknown permission ids {} to role {}", missingIds, id);
            throw new BusinessException("Permission IDs not found: " + missingIds);
        }

        new ArrayList<>(role.getPermissions()).forEach(role::removePermission);
        permissions.forEach(role::addPermission);

        Role saved = roleRepository.save(role);
        log.info("Assigned {} permission(s) to role {}", permissions.size(), id);

        return roleMapper.toResponse(saved);
    }

    private Role findRoleById(Long id) {

        return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

    }
}
