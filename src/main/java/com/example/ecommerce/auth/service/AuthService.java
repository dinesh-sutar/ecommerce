package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.AuthResponse;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.exception.DuplicateEmailException;
import com.example.ecommerce.exception.InvalidCredentialsException;
import com.example.ecommerce.exception.InvalidRefreshTokenException;
import com.example.ecommerce.security.JwtService;
import com.example.ecommerce.user.dto.UserResponse;
import com.example.ecommerce.user.entity.RefreshToken;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.RefreshTokenRepository;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final RefreshTokenRepository refreshTokenRepository;
        @Value("${jwt.refresh-token-expiration}")
        private long refreshTokenExpiration;

        public AuthResponse register(RegisterRequest request) {

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new DuplicateEmailException("Email already registered");
                }

                User user = User.builder()
                                .name(request.getName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .phone(request.getPhone())
                                .build();

                User savedUser = userRepository.save(user);

                String accessToken = jwtService.generateAccessToken(
                                savedUser.getEmail());

                String refreshToken = jwtService.generateRefreshToken(savedUser.getEmail());

                RefreshToken refreshTokenEntity = RefreshToken.builder()
                                .token(refreshToken)
                                .expiryDate(
                                                LocalDateTime.now()
                                                                .plusNanos(refreshTokenExpiration * 1_000_000))
                                .revoked(false)
                                .user(savedUser)
                                .build();

                refreshTokenRepository.save(refreshTokenEntity);

                UserResponse userResponse = UserResponse.builder()
                                .id(savedUser.getId())
                                .name(savedUser.getName())
                                .email(savedUser.getEmail())
                                .phone(savedUser.getPhone())
                                .build();

                return AuthResponse.builder()
                                .user(userResponse)
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .build();
        }

        public AuthResponse login(LoginRequest request) {

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

                if (!passwordEncoder.matches(
                                request.getPassword(),
                                user.getPassword())) {
                        throw new InvalidCredentialsException("Invalid email or password");
                }

                String accessToken = jwtService.generateAccessToken(user.getEmail());

                String refreshToken = jwtService.generateRefreshToken(user.getEmail());

                RefreshToken refreshTokenEntity = RefreshToken.builder()
                                .token(refreshToken)
                                .expiryDate(
                                                LocalDateTime.now()
                                                                .plusNanos(refreshTokenExpiration * 1_000_000))
                                .revoked(false)
                                .user(user)
                                .build();

                refreshTokenRepository.save(refreshTokenEntity);

                UserResponse userResponse = UserResponse.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .build();

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .user(userResponse)
                                .build();
        }

        public AuthResponse refreshAccessToken(String token) {

                RefreshToken refreshToken = refreshTokenRepository
                                .findByToken(token)
                                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

                if (refreshToken.isRevoked()) {
                        throw new InvalidRefreshTokenException("Refresh token has been revoked");
                }

                if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                        throw new InvalidRefreshTokenException("Refresh token has expired");
                }

                if (!jwtService.isTokenValid(token)) {
                        throw new InvalidRefreshTokenException("Invalid refresh token");
                }

                User user = refreshToken.getUser();

                String newAccessToken = jwtService.generateAccessToken(user.getEmail());

                UserResponse userResponse = UserResponse.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .build();

                return AuthResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(token)
                                .user(userResponse)
                                .build();
        }

        public void logout(String token) {

                RefreshToken refreshToken = refreshTokenRepository
                                .findByToken(token)
                                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

                refreshToken.setRevoked(true);

                refreshTokenRepository.save(refreshToken);
        }
}