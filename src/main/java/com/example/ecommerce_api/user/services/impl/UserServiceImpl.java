package com.example.ecommerce_api.user.services.impl;

import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.exception.UserDuplicationException;
import com.example.ecommerce_api.user.dto.request.UpdatePasswordRequest;
import com.example.ecommerce_api.user.dto.request.UpdateProfileRequest;
import com.example.ecommerce_api.user.dto.request.UserRequest;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import com.example.ecommerce_api.user.entity.Address;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.mapper.AddressMapper;
import com.example.ecommerce_api.user.mapper.UserMapper;
import com.example.ecommerce_api.user.repository.UserRepository;
import com.example.ecommerce_api.user.services.UserService;
import com.example.ecommerce_api.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse createUser(UserRequest userRequest) {


        if (userRepository.existsByEmailIgnoreCase(userRequest.getEmail())) {
            log.warn("Attempted to create user with duplicate email '{}'", userRequest.getEmail());
            throw new UserDuplicationException("Email already exists");
        }

        if (!userRequest.getPassword().equals(userRequest.getConfirmPassword())) {
            throw new BusinessException("Password and confirm password do not match");
        }

        User user = userMapper.toEntity(userRequest);

        if (userRequest.getAddress() != null) {

            Address address = addressMapper.toEntity(userRequest.getAddress());
            user.addAddress(address);
        }

        User savedUser = userRepository.save(user);

        log.info("Created user {} with id {}", savedUser.getEmail(), savedUser.getId());

        return userMapper.toResponse(savedUser) ;
    }

    @Override
    public UserResponse updateUser(Long id ,UserRequest userRequest) {

        User user = userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found"));



        if (userRequest.getEmail() !=null
                && !userRequest.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCase(userRequest.getEmail()))
        {
            log.warn("Attempted to update user {} to duplicate email '{}'", id, userRequest.getEmail());
            throw new UserDuplicationException("Email already exists");
        }

//        if (userRequest.getAddress() !=null && userRepository.existsByEmailIgnoreCaseAndIdNot(userRequest.getEmail(),id))
//        {
//            throw new UserDuplicationException("Email already exists");
//        }

        userMapper.updateEntity(user,userRequest);

        User savedUser = userRepository.save(user);
        log.info("Updated user {}", id);
        return userMapper.toResponse(savedUser) ;
    }

    @Override
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userMapper.updateProfile(user, request);

        User savedUser = userRepository.save(user);
        log.info("Updated profile for user {}", id);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse updatePassword(Long id, UpdatePasswordRequest userRequest) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(userRequest.getOldPassword(), user.getPassword())) {
            log.warn("Failed password update attempt for user {}: incorrect old password", id);
            throw new BusinessException("Old password is incorrect");
        }

        if (!userRequest.getNewPassword().equals(userRequest.getConfirmNewPassword())) {
            throw new BusinessException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(userRequest.getNewPassword()));

        User savedUser = userRepository.save(user);
        log.info("Password updated for user {}", id);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse findUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse findUserByEmail(String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
        log.info("Deleted user {}", id);
    }

    @Override
    public void deactivateUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user {}", id);
    }
}
