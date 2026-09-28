package com.example.ecommerce_api.user.services;

import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.user.dto.request.AssignPermissionsRequest;
import com.example.ecommerce_api.user.dto.request.RoleRequest;
import com.example.ecommerce_api.user.dto.response.RoleResponse;
import com.example.ecommerce_api.user.entity.Role;

import java.util.List;

public interface RoleService {


    RoleResponse createRole(RoleRequest roleRequest);
    RoleResponse updateRole(Long id,RoleRequest roleRequest);

    void deleteRole(Long id);
    List<RoleResponse> findAll();
    RoleResponse findById(Long id);

    RoleResponse assignPermissions(Long id, AssignPermissionsRequest request);


}
