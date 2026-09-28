package com.example.ecommerce_api.common.exception;

/**
 * The presented refresh token does not correspond to any known, active session
 * (unknown hash, or already revoked through normal logout).
 */
public class InvalidRefreshTokenException extends RefreshTokenException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
