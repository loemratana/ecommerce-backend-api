package com.example.ecommerce_api.common.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message,Object value)
    {
        super(message.replace("{}",String.valueOf(value)));
    }
}
