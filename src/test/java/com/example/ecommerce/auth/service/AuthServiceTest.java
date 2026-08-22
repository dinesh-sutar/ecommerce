package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.AuthResponse;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.exception.DuplicateEmailException;
import com.example.ecommerce.exception.InvalidCredentialsException;
import com.example.ecommerce.exception.InvalidRefreshTokenException;
import com.example.ecommerce.security.JwtService;
import com.example.ecommerce.user.entity.RefreshToken;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.RefreshTokenRepository;
import com.example.ecommerce.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                authService,
                "refreshTokenExpiration",
                604800000L);

        user = User.builder()
                .id(1L)
                .name("Dinesh")
                .email("dinesh@test.com")
                .password("encodedPassword")
                .phone("9876543210")
                .build();
    }

    // =========================================================
    // REGISTER TESTS
    // =========================================================

    @Test
    void register_ShouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Dinesh");
        request.setEmail("dinesh@test.com");
        request.setPassword("password123");
        request.setPhone("9876543210");

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        when(jwtService.generateAccessToken(user.getEmail()))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(user.getEmail()))
                .thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);

        assertEquals("access-token",
                response.getAccessToken());

        assertEquals("refresh-token",
                response.getRefreshToken());

        assertNotNull(response.getUser());

        assertEquals(1L,
                response.getUser().getId());

        assertEquals("Dinesh",
                response.getUser().getName());

        assertEquals("dinesh@test.com",
                response.getUser().getEmail());

        assertEquals("9876543210",
                response.getUser().getPhone());

        verify(userRepository)
                .existsByEmail("dinesh@test.com");

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));

        verify(jwtService)
                .generateAccessToken("dinesh@test.com");

        verify(jwtService)
                .generateRefreshToken("dinesh@test.com");

        verify(refreshTokenRepository)
                .save(any(RefreshToken.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Dinesh");
        request.setEmail("dinesh@test.com");
        request.setPassword("password123");
        request.setPhone("9876543210");

        when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> authService.register(request));

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(jwtService, never())
                .generateAccessToken(anyString());

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    @Test
    void register_ShouldSaveRefreshToken() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Dinesh");
        request.setEmail("dinesh@test.com");
        request.setPassword("password123");
        request.setPhone("9876543210");

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        when(jwtService.generateAccessToken(anyString()))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(anyString()))
                .thenReturn("refresh-token");

        authService.register(request);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository)
                .save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        assertEquals("refresh-token",
                savedToken.getToken());

        assertFalse(savedToken.isRevoked());

        assertEquals(user,
                savedToken.getUser());

        assertNotNull(
                savedToken.getExpiryDate());
    }

    // =========================================================
    // LOGIN TESTS
    // =========================================================

    @Test
    void login_ShouldLoginSuccessfully() {

        LoginRequest request = new LoginRequest();
        request.setEmail("dinesh@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encodedPassword")).thenReturn(true);

        when(jwtService.generateAccessToken(user.getEmail()))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(user.getEmail()))
                .thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);

        assertEquals("access-token",
                response.getAccessToken());

        assertEquals("refresh-token",
                response.getRefreshToken());

        assertEquals(user.getId(),
                response.getUser().getId());

        assertEquals(user.getEmail(),
                response.getUser().getEmail());

        verify(userRepository)
                .findByEmail("dinesh@test.com");

        verify(passwordEncoder)
                .matches(
                        "password123",
                        "encodedPassword");

        verify(refreshTokenRepository)
                .save(any(RefreshToken.class));
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request));

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateAccessToken(anyString());

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    @Test
    void login_ShouldThrowException_WhenPasswordIsIncorrect() {

        LoginRequest request = new LoginRequest();
        request.setEmail("dinesh@test.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encodedPassword")).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request));

        verify(jwtService, never())
                .generateAccessToken(anyString());

        verify(jwtService, never())
                .generateRefreshToken(anyString());

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    @Test
    void login_ShouldSaveRefreshToken() {

        LoginRequest request = new LoginRequest();
        request.setEmail("dinesh@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        when(jwtService.generateAccessToken(anyString()))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(anyString()))
                .thenReturn("refresh-token");

        authService.login(request);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository)
                .save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        assertEquals("refresh-token",
                savedToken.getToken());

        assertFalse(savedToken.isRevoked());

        assertEquals(user,
                savedToken.getUser());

        assertNotNull(
                savedToken.getExpiryDate());
    }

    // =========================================================
    // REFRESH ACCESS TOKEN TESTS
    // =========================================================

    @Test
    void refreshAccessToken_ShouldGenerateNewAccessToken() {

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .expiryDate(
                        LocalDateTime.now()
                                .plusDays(7))
                .revoked(false)
                .user(user)
                .build();

        when(refreshTokenRepository
                .findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        when(jwtService.isTokenValid(
                "valid-refresh-token"))
                .thenReturn(true);

        when(jwtService.generateAccessToken(
                user.getEmail()))
                .thenReturn("new-access-token");

        AuthResponse response = authService.refreshAccessToken(
                "valid-refresh-token");

        assertNotNull(response);

        assertEquals(
                "new-access-token",
                response.getAccessToken());

        assertEquals(
                "valid-refresh-token",
                response.getRefreshToken());

        assertEquals(
                user.getId(),
                response.getUser().getId());

        verify(jwtService)
                .isTokenValid(
                        "valid-refresh-token");

        verify(jwtService)
                .generateAccessToken(
                        user.getEmail());
    }

    @Test
    void refreshAccessToken_ShouldThrowException_WhenTokenNotFound() {

        when(refreshTokenRepository
                .findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshAccessToken(
                        "invalid-token"));

        verify(jwtService, never())
                .isTokenValid(anyString());

        verify(jwtService, never())
                .generateAccessToken(anyString());
    }

    @Test
    void refreshAccessToken_ShouldThrowException_WhenTokenIsRevoked() {

        RefreshToken refreshToken = RefreshToken.builder()
                .token("revoked-token")
                .expiryDate(
                        LocalDateTime.now()
                                .plusDays(7))
                .revoked(true)
                .user(user)
                .build();

        when(refreshTokenRepository
                .findByToken("revoked-token"))
                .thenReturn(Optional.of(refreshToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshAccessToken(
                        "revoked-token"));

        verify(jwtService, never())
                .isTokenValid(anyString());

        verify(jwtService, never())
                .generateAccessToken(anyString());
    }

    @Test
    void refreshAccessToken_ShouldThrowException_WhenTokenExpired() {

        RefreshToken refreshToken = RefreshToken.builder()
                .token("expired-token")
                .expiryDate(
                        LocalDateTime.now()
                                .minusDays(1))
                .revoked(false)
                .user(user)
                .build();

        when(refreshTokenRepository
                .findByToken("expired-token"))
                .thenReturn(Optional.of(refreshToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshAccessToken(
                        "expired-token"));

        verify(jwtService, never())
                .isTokenValid(anyString());

        verify(jwtService, never())
                .generateAccessToken(anyString());
    }

    @Test
    void refreshAccessToken_ShouldThrowException_WhenJwtIsInvalid() {

        RefreshToken refreshToken = RefreshToken.builder()
                .token("invalid-jwt-token")
                .expiryDate(
                        LocalDateTime.now()
                                .plusDays(7))
                .revoked(false)
                .user(user)
                .build();

        when(refreshTokenRepository
                .findByToken("invalid-jwt-token"))
                .thenReturn(Optional.of(refreshToken));

        when(jwtService.isTokenValid(
                "invalid-jwt-token"))
                .thenReturn(false);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshAccessToken(
                        "invalid-jwt-token"));

        verify(jwtService, never())
                .generateAccessToken(anyString());
    }

    // =========================================================
    // LOGOUT TESTS
    // =========================================================

    @Test
    void logout_ShouldRevokeRefreshToken() {

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .expiryDate(
                        LocalDateTime.now()
                                .plusDays(7))
                .revoked(false)
                .user(user)
                .build();

        when(refreshTokenRepository
                .findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        authService.logout("refresh-token");

        assertTrue(refreshToken.isRevoked());

        verify(refreshTokenRepository)
                .save(refreshToken);
    }

    @Test
    void logout_ShouldThrowException_WhenTokenDoesNotExist() {

        when(refreshTokenRepository
                .findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.logout(
                        "invalid-token"));

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }
}