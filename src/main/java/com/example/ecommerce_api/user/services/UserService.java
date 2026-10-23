package com.example.ecommerce_api.user.services;

import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.user.dto.request.UpdatePasswordRequest;
import com.example.ecommerce_api.user.dto.request.UpdateProfileRequest;
import com.example.ecommerce_api.user.dto.request.UpdateUserRequest;
import com.example.ecommerce_api.user.dto.request.UserRequest;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse createUser(UserRequest userRequest);
    UserResponse updateUser(Long id, UpdateUserRequest userRequest);

    PageResponse<UserResponse> getAllUser(Boolean active,
                                          String search, Pageable pageable);

    UserResponse updateProfile(Long id, UpdateProfileRequest request);

    UserResponse updateAvatar(Long id, MultipartFile file);

    UserResponse updatePassword(Long id, UpdatePasswordRequest userRequest);

    UserResponse findUserById(Long id);
    UserResponse findUserByEmail(String email);
    void deleteUser(Long id );
    void deactivateUser(Long id);

    PageResponse<UserResponse> getAllUsers(Pageable pageable);
}
