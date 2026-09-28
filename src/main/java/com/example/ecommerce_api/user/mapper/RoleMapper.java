package com.example.ecommerce_api.user.mapper;


import com.example.ecommerce_api.permission.mapper.PermissionMapper;
import com.example.ecommerce_api.user.dto.request.RoleRequest;
import com.example.ecommerce_api.user.dto.response.RoleResponse;
import com.example.ecommerce_api.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final PermissionMapper permissionMapper;


    public Role toEntity(RoleRequest request)
    {
        Role role = new Role();
        role.setName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setActive(true);
        return role;

    }

    public Role updateEntity(Role role, RoleRequest request)
    {
        if (request == null) {
            return role;
        }

        if (request.getRoleName() !=null)
        {
            role.setName(request.getRoleName());
        }
        if (request.getDescription() !=null)
        {
            role.setDescription(request.getDescription());
        }
        return role;
    }

    public RoleResponse toResponse(Role role)
    {
       return RoleResponse.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .active(role.isActive())
                .description(role.getDescription())
                .permissions(permissionMapper.toResponseSet(role.getPermissions()))
                .build();

    }


    public List<RoleResponse> toResponseList(List<Role> roles)
    {
        return roles.stream().map(this::toResponse).toList();
    }
}
