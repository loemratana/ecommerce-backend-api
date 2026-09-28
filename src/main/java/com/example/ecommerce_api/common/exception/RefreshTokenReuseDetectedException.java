package com.example.ecommerce_api.common.exception;

/**
 * A refresh token that had already been rotated (and therefore revoked) was
 * presented again. This indicates the token was very likely stolen and
 * replayed, so the whole device session is revoked as a side effect.
 */
public class RefreshTokenReuseDetectedException extends RefreshTokenException {
    public RefreshTokenReuseDetectedException(String message) {
        super(message);
    }
}
