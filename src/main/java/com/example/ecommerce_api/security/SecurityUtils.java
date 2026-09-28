package com.example.ecommerce_api.security;

import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.user.entity.User;
import com.example.ecommerce_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component("securityUtils")
@RequiredArgsConstructor
public  class SecurityUtils {
    private final UserRepository userRepository;


    public User getCurrentUser() {
        // Retrieve the Authentication object for the currently authenticated user.
        // It contains information such as the username, authorities/roles, and authentication status.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal()))
        {
            throw new BadCredentialsException(
                    "Not authenticated"
            );
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );


    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
