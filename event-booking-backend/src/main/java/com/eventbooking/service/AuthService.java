package com.eventbooking.service;

import com.eventbooking.dto.AuthResponseDto;
import com.eventbooking.dto.LoginRequestDto;
import com.eventbooking.dto.RegisterRequestDto;
import com.eventbooking.dto.UserResponseDto;
import com.eventbooking.entity.RefreshToken;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BadRequestException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.RefreshTokenRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRevocationService tokenRevocationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : com.eventbooking.enums.Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        return generateAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponseDto refreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            // Theft detection! Revoke entire token family in isolated transaction
            tokenRevocationService.revokeFamily(storedToken.getFamilyId());
            throw new BadRequestException("Token reuse detected! All sessions revoked for security.");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }

        // Revoke old token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Issue new token pair under same familyId
        return generateAuthResponse(user, storedToken.getFamilyId());
    }

    public UserResponseDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponseDto generateAuthResponse(User user) {
        return generateAuthResponse(user, UUID.randomUUID().toString());
    }

    private AuthResponseDto generateAuthResponse(User user, String familyId) {
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        String rawRefreshToken = UUID.randomUUID().toString() + "." + UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hashToken(rawRefreshToken))
                .familyId(familyId)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void revokeFamily(String familyId) {
        var family = refreshTokenRepository.findByFamilyId(familyId);
        family.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAllAndFlush(family);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
