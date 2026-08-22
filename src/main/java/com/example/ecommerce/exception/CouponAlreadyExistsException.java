package com.example.ecommerce.exception;

public class CouponAlreadyExistsException extends RuntimeException {

    public CouponAlreadyExistsException(String message) {
        super(message);
    }
}