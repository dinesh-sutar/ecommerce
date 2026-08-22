package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.CheckoutRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request) {

        return orderService.checkout(
                authentication.getName(),
                request);
    }

    @GetMapping
    public List<OrderResponse> getUserOrders(
            Authentication authentication) {

        return orderService.getUserOrders(
                authentication.getName());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(
            Authentication authentication,
            @PathVariable Long orderId) {

        return orderService.getOrderById(
                authentication.getName(),
                orderId);
    }
}