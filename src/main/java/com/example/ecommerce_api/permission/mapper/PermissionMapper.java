package com.example.ecommerce_api.permission.mapper;

import com.example.ecommerce_api.permission.dto.request.PermissionRequest;
import com.example.ecommerce_api.permission.dto.response.PermissionResponse;
import com.example.ecommerce_api.permission.entity.Permission;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PermissionMapper {

    public Permission toEntity(PermissionRequest request) {
        return Permission.builder()
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();
    }

    public Permission updateEntity(Permission permission, PermissionRequest request) {
        if (request == null) {
            return permission;
        }

        if (request.getName() != null) {
            permission.setName(request.getName());
        }
        if (request.getResource() != null) {
            permission.setResource(request.getResource());
        }
        if (request.getAction() != null) {
            permission.setAction(request.getAction());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
        return permission;
    }

    public PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }

    public List<PermissionResponse> toResponseList(List<Permission> permissions) {
        return permissions.stream().map(this::toResponse).toList();
    }

    public Set<PermissionResponse> toResponseSet(Set<Permission> permissions) {
        if (permissions == null) {
            return Set.of();
        }
        return permissions.stream().map(this::toResponse).collect(Collectors.toSet());
    }
}
