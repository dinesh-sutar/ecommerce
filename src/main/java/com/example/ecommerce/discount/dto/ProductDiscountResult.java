package com.example.ecommerce.discount.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDiscountResult {

    private Long discountId;

    private String discountCode;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private boolean applied;
}