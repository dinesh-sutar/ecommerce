package com.example.ecommerce.product.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductImageResponse {

    private Long id;

    private String imageUrl;

    private String fileName;

    private Boolean isPrimary;
}