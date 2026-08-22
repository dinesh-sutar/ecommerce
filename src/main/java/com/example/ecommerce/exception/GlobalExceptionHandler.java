package com.example.ecommerce.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(DuplicateEmailException.class)
        public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
                        DuplicateEmailException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.CONFLICT,
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
                        InvalidCredentialsException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(
                        InvalidRefreshTokenException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                String message = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                message,
                                request);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage(),
                                request);
        }

        @ExceptionHandler(InsufficientStockException.class)
        public ResponseEntity<ApiErrorResponse> handleInsufficientStock(
                        InsufficientStockException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage(),
                                request);
        }

        private ResponseEntity<ApiErrorResponse> buildResponse(
                        HttpStatus status,
                        String message,
                        HttpServletRequest request) {

                ApiErrorResponse response = ApiErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(status.value())
                                .error(status.getReasonPhrase())
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity
                                .status(status)
                                .body(response);
        }
}