package com.example.ecommerce.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {

    private Long productId;

    private String productName;

    private String sku;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    private BigDecimal discountAmount;

    private BigDecimal totalPrice;
}