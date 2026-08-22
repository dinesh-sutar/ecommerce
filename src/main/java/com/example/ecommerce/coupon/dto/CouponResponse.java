package com.example.ecommerce.coupon.dto;

import com.example.ecommerce.discount.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {

    private Long id;

    private String code;

    private DiscountType type;

    private BigDecimal value;

    private BigDecimal minimumCartValue;

    private BigDecimal maximumDiscount;

    private LocalDateTime expiryDate;

    private Boolean active;
}