package com.example.ecommerce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final String secret = "my-super-secret-key-for-jwt-testing-which-is-long-enough-123456";

    private final long accessTokenExpiration = 60 * 60 * 1000; // 1 hour

    private final long refreshTokenExpiration = 24 * 60 * 60 * 1000; // 1 day

    @BeforeEach
    void setUp() {

        jwtService = new JwtService(
                secret,
                accessTokenExpiration,
                refreshTokenExpiration);
    }

    @Test
    void generateAccessToken_ShouldReturnToken() {

        String email = "dinesh@example.com";

        String token = jwtService.generateAccessToken(email);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateRefreshToken_ShouldReturnToken() {

        String email = "dinesh@example.com";

        String token = jwtService.generateRefreshToken(email);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {

        String email = "dinesh@example.com";

        String token = jwtService.generateAccessToken(email);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void isTokenValid_ShouldReturnTrue_ForValidToken() {

        String token = jwtService.generateAccessToken("dinesh@example.com");

        boolean valid = jwtService.isTokenValid(token);

        assertTrue(valid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForInvalidToken() {

        String invalidToken = "this.is.not.a.valid.jwt.token";

        boolean valid = jwtService.isTokenValid(invalidToken);

        assertFalse(valid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForTamperedToken() {

        String token = jwtService.generateAccessToken("dinesh@example.com");

        String tamperedToken = token + "tampered";

        boolean valid = jwtService.isTokenValid(tamperedToken);

        assertFalse(valid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForExpiredToken()
            throws InterruptedException {

        JwtService expiredJwtService = new JwtService(
                secret,
                1,
                1);

        String token = expiredJwtService.generateAccessToken(
                "dinesh@example.com");

        Thread.sleep(10);

        boolean valid = expiredJwtService.isTokenValid(token);

        assertFalse(valid);
    }
}