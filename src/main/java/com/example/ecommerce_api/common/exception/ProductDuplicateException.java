package com.example.ecommerce_api.common.exception;

public class ProductDuplicateException extends RuntimeException {
    public ProductDuplicateException(String message) {
        super(message);
    }
}
