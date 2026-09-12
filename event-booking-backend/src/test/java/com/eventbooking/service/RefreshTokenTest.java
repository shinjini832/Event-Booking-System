package com.eventbooking.service;

import com.eventbooking.dto.AuthResponseDto;
import com.eventbooking.dto.RegisterRequestDto;
import com.eventbooking.enums.Role;
import com.eventbooking.exception.BadRequestException;
import com.eventbooking.repository.RefreshTokenRepository;
import com.eventbooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private AuthResponseDto initialAuth;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequestDto register = new RegisterRequestDto();
        register.setEmail("theft.test@event.com");
        register.setPassword("securePass123");
        register.setRole(Role.USER);

        initialAuth = authService.register(register);
    }

    @Test
    @DisplayName("Refresh Token Rotation: Valid refresh token issues new pair and revokes old token")
    void testNormalRefreshTokenRotation() {
        String oldRefreshToken = initialAuth.getRefreshToken();
        AuthResponseDto refreshed = authService.refreshToken(oldRefreshToken);

        assertNotNull(refreshed.getAccessToken());
        assertNotNull(refreshed.getRefreshToken());
        assertNotEquals(oldRefreshToken, refreshed.getRefreshToken());
    }

    @Test
    @DisplayName("Re-use Theft Detection: Presenting a revoked refresh token revokes entire token family")
    void testRefreshTokenReuseTheftDetection() {
        String originalToken = initialAuth.getRefreshToken();

        // 1. Legitimate user rotates token
        AuthResponseDto legitimateRotation = authService.refreshToken(originalToken);
        assertNotNull(legitimateRotation.getAccessToken());

        // 2. Attacker attempts to reuse old originalToken
        assertThrows(BadRequestException.class, () -> authService.refreshToken(originalToken));

        // 3. Legitimate user now tries to use their new token -> should also be revoked due to family revocation!
        assertThrows(BadRequestException.class, () -> authService.refreshToken(legitimateRotation.getRefreshToken()));
    }
}
