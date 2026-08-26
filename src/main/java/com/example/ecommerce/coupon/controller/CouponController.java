package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.coupon.dto.CouponResponse;
import com.example.ecommerce.coupon.dto.CreateCouponRequest;
import com.example.ecommerce.coupon.service.CouponService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // Create Coupon
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {

        return couponService.createCoupon(request);
    }
}