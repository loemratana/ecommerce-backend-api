package com.example.ecommerce_api.user.services.impl;

import com.example.ecommerce_api.common.exception.InvalidRefreshTokenException;
import com.example.ecommerce_api.common.exception.RefreshTokenExpiredException;
import com.example.ecommerce_api.common.exception.RefreshTokenReuseDetectedException;
import com.example.ecommerce_api.user.entity.RefreshToken;
import com.example.ecommerce_api.user.entity.UserDevice;
import com.example.ecommerce_api.user.repository.RefreshTokenRepository;
import com.example.ecommerce_api.user.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    /** 512 bits of entropy per token. */
    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public IssuedToken issue(UserDevice device) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenTtlDays, ChronoUnit.DAYS);
        String rawToken = generateRawToken();

        RefreshToken token = RefreshToken.builder()
                .device(device)
                .tokenHash(hash(rawToken))
                .createdAt(now)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);
        log.debug("Issued refresh token for device {}", device.getId());
        return new IssuedToken(rawToken, expiresAt);
    }

    @Override
    @Transactional
    public RotatedToken rotate(String rawRefreshToken) {
        Instant now = Instant.now();
        String incomingHash = hash(rawRefreshToken);

        // Row lock serializes concurrent refreshes of the same token: the loser
        // of the race observes revoked = true below and is treated as reuse.
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(incomingHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        if (current.isRevoked()) {
            log.warn("Refresh token reuse detected for device {}", current.getDevice().getId());
            refreshTokenRepository.revokeAllForDevice(current.getDevice().getId(), now);
            throw new RefreshTokenReuseDetectedException("Refresh token has already been used; session revoked");
        }

        if (current.isExpired(now)) {
            log.warn("Refresh attempted with expired token for device {}", current.getDevice().getId());
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        String newRawToken = generateRawToken();
        Instant expiresAt = now.plus(refreshTokenTtlDays, ChronoUnit.DAYS);

        RefreshToken next = RefreshToken.builder()
                .device(current.getDevice())
                .tokenHash(hash(newRawToken))
                .createdAt(now)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(next);

        current.setRevoked(true);
        current.setRevokedAt(now);
        current.setLastUsedAt(now);
        current.setReplacedBy(next);

        log.debug("Rotated refresh token for device {}", current.getDevice().getId());

        return new RotatedToken(newRawToken, expiresAt, current.getDevice());
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        String incomingHash = hash(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        if (!token.isRevoked()) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            log.info("Revoked refresh token for device {}", token.getDevice().getId());
        }
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        log.info("Revoked all refresh tokens for user {}", userId);
    }

    @Override
    @Transactional
    public void revokeAllForDevice(Long deviceId) {
        refreshTokenRepository.revokeAllForDevice(deviceId, Instant.now());
        log.info("Revoked all refresh tokens for device {}", deviceId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
