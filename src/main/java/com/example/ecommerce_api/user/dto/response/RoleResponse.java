package com.example.ecommerce_api.user.dto.response;



import com.example.ecommerce_api.permission.dto.response.PermissionResponse;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class RoleResponse {
    private Long roleId;
    private String roleName;
    private boolean active;
    private String description;
    private Set<PermissionResponse> permissions;

}
