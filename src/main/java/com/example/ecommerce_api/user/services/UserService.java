package com.example.ecommerce_api.user.services;

import com.example.ecommerce_api.user.dto.request.UpdatePasswordRequest;
import com.example.ecommerce_api.user.dto.request.UpdateProfileRequest;
import com.example.ecommerce_api.user.dto.request.UserRequest;
import com.example.ecommerce_api.user.dto.response.UserResponse;

public interface UserService {

    UserResponse createUser(UserRequest userRequest);
    UserResponse updateUser(Long id ,UserRequest userRequest);

    UserResponse updateProfile(Long id, UpdateProfileRequest request);

    UserResponse updatePassword(Long id, UpdatePasswordRequest userRequest);

    UserResponse findUserById(Long id);
    UserResponse findUserByEmail(String email);
    void deleteUser(Long id );
    void deactivateUser(Long id);
}
