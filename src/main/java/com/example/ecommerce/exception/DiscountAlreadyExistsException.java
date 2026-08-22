package com.example.ecommerce.exception;

public class DiscountAlreadyExistsException extends RuntimeException {

    public DiscountAlreadyExistsException(String message) {
        super(message);
    }
}