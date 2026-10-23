package com.example.ecommerce_api.common.exception;

/**
 * Thrown when a stock operation asks for more units than are available.
 * Extends BadRequestException so it is returned as 400 by GlobalExceptionHandler.
 */
public class InsufficientStockException extends BadRequestException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
