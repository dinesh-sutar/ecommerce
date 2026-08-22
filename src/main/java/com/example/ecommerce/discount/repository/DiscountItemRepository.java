package com.example.ecommerce.discount.repository;

import com.example.ecommerce.discount.entity.DiscountItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountItemRepository extends JpaRepository<DiscountItem, Long> {

    List<DiscountItem> findByProductId(Long productId);
}