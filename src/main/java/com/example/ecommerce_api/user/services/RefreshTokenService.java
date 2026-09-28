package com.example.ecommerce_api.user.services;

import com.example.ecommerce_api.user.entity.UserDevice;

import java.time.Instant;

/**
 * Owns the lifecycle of opaque refresh tokens: issuing, hashing, rotating,
 * revoking, and reuse detection. Deliberately separate from AuthService so
 * that token-storage concerns don't leak into login/logout orchestration.
 */
public interface RefreshTokenService {

    IssuedToken issue(UserDevice device);

    RotatedToken rotate(String rawRefreshToken);

    void revoke(String rawRefreshToken);

    void revokeAllForUser(Long userId);

    void revokeAllForDevice(Long deviceId);

    record IssuedToken(String rawToken, Instant expiresAt) {
    }

    record RotatedToken(String rawToken, Instant expiresAt, UserDevice device) {
    }
}
