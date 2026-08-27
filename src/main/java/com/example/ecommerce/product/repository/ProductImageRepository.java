package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductId(Long productId);

    long countByProductId(Long productId);

    List<ProductImage> findByProductIdOrderByIsPrimaryDesc(
            Long productId);
}