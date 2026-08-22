package com.example.ecommerce.exception;

public class InvalidDiscountException extends RuntimeException {

    public InvalidDiscountException(String message) {
        super(message);
    }
}