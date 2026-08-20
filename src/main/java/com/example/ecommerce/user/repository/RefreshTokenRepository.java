package com.example.ecommerce.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerce.user.entity.RefreshToken;
import com.example.ecommerce.user.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

    void deleteByUserAndToken(User user, String token);
    
}
