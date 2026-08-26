package com.example.ecommerce.address.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerce.address.entity.UserAddress;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserId(Long userId);

    Optional<UserAddress> findByIdAndUserId(
            Long addressId,
            Long userId);
}