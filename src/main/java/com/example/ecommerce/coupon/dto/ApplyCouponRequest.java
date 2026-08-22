package com.example.ecommerce.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;
}