package com.example.ecommerce_api.common.exception;

public class RefreshTokenExpiredException extends RefreshTokenException {
    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
