package com.example.ecommerce_api.user.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImage;
    private boolean active;

    private List<AddressResponse> addresses;
    private Set<RoleResponse> role;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
