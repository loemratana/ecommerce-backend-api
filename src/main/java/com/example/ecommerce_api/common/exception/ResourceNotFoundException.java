package com.example.ecommerce_api.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    private Long id;
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message , Long id)
    {
        super(message);
        this.id = id;
    }
}
