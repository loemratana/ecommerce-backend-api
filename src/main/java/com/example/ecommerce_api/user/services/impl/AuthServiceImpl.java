package com.example.ecommerce_api.user.services.impl;

import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.security.jwt.JwtUtils;
import com.example.ecommerce_api.security.service.UserDetailsImpl;
import com.example.ecommerce_api.user.dto.request.LoginRequest;
import com.example.ecommerce_api.user.dto.request.RegisterRequest;
import com.example.ecommerce_api.user.dto.response.AuthResponse;
import com.example.ecommerce_api.user.dto.response.UserResponse;
import com.example.ecommerce_api.user.entity.Role;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.entity.UserDevice;
import com.example.ecommerce_api.user.mapper.UserMapper;
import com.example.ecommerce_api.user.repository.RoleRepository;
import com.example.ecommerce_api.user.repository.UserRepository;
import com.example.ecommerce_api.user.services.AuthService;
import com.example.ecommerce_api.user.services.RefreshTokenService;
import com.example.ecommerce_api.user.services.UserDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserDeviceService userDeviceService;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDevice device = userDeviceService.findOrCreateDevice(
                user, request.getDeviceId(), request.getDeviceName(), request.getDeviceType());

        String accessToken = jwtUtils.generateAccessToken(user.getEmail());
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(device);

        log.info("User {} logged in from device {}", user.getEmail(), device.getId());

        return buildResponse(accessToken, refreshToken.rawToken());
    }


    @Override
    public UserResponse register(RegisterRequest registerRequest) {

        if (registerRequest.getEmail() !=null && userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())) {

            log.warn("Attempted to register with duplicate email '{}'", registerRequest.getEmail());
            throw new BadRequestException("Email already registered");

        }

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword()))
        {
            throw new BadRequestException("Passwords do not match");
        }

        Role role = roleRepository.findByNameIgnoreCase("CUSTOMER").orElseThrow(
                () -> new ResourceNotFoundException("Role not found")
        );

        User user = new User();

        user.setEmail(registerRequest.getEmail());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setActive(true);
        user.setRoles(Set.of(role));

        User save = userRepository.save(user);

        log.info("Registered new user {} with id {}", save.getEmail(), save.getId());

        return userMapper.toResponse(save);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);

        UserDevice device = rotated.device();
        device.touch(Instant.now());

        String accessToken = jwtUtils.generateAccessToken(device.getUser().getEmail());

        log.debug("Refreshed session for device {}", device.getId());

        return buildResponse(accessToken, rotated.rawToken());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
        log.info("Refresh token revoked (logout)");
    }

    @Override
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
        log.info("All sessions revoked for user {}", userId);
    }

    private AuthResponse buildResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getAccessTokenTtlSeconds())
                .build();
    }
}
