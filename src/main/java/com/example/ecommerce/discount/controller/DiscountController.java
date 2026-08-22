package com.example.ecommerce.discount.controller;

import com.example.ecommerce.discount.dto.CreateDiscountRequest;
import com.example.ecommerce.discount.dto.DiscountResponse;
import com.example.ecommerce.discount.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountResponse> createDiscount(
            @Valid @RequestBody CreateDiscountRequest request) {

        DiscountResponse response = discountService.createDiscount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}