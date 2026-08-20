package com.example.ecommerce.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductResponse {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String category;
}