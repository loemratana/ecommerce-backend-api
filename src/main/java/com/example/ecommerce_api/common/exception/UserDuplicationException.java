package com.example.ecommerce_api.common.exception;

public class UserDuplicationException  extends RuntimeException {
    public UserDuplicationException(String message) {
        super(message);
    }
}
