package com.example.ecommerce.discount.repository;

import com.example.ecommerce.discount.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCode(String code);

    boolean existsByCode(String code);
}