package com.example.ecommerce_api.user.mapper;


import com.example.ecommerce_api.user.dto.request.UpdateProfileRequest;
import com.example.ecommerce_api.user.dto.request.UserRequest;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import com.example.ecommerce_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final RoleMapper roleMapper;
    private final AddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;

    public User toEntity(UserRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProfileImage(request.getProfilePicture());
        user.setActive(true);
        return user;
    }

    public User updateEntity(User user, UserRequest request) {
        if (request == null) {
            return user;
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getProfilePicture() != null) {
            user.setProfileImage(request.getProfilePicture());
        }
        return user;
    }

    public User updateProfile(User user, UpdateProfileRequest request) {
        if (request == null) {
            return user;
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getProfileImage() != null) {
            user.setProfileImage(request.getProfileImage());
        }
        return user;
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .active(user.isActive())
                .addresses(addressMapper.toResponseList(user.getAddresses()))
                .role(user.getRoles().stream()
                        .map(roleMapper::toResponse)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public List<UserResponse> toResponseList(List<User> users) {
        return users.stream().map(this::toResponse).toList();
    }
}
