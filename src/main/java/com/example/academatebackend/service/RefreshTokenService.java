package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.UnauthorizedException;
import com.example.academatebackend.entity.RefreshToken;
import com.example.academatebackend.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    @Transactional
    public String generate(UUID userId, HttpServletRequest request) {
        String plainToken = UUID.randomUUID().toString();
        String tokenHash = hash(plainToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .deviceInfo(truncate(request.getHeader("User-Agent"), 255))
                .ipAddress(getClientIp(request))
                .expiresAt(Instant.now().plus(refreshTokenExpiration))
                .build();

        refreshTokenRepository.save(refreshToken);
        return plainToken;
    }

    @Transactional
    public RefreshToken validate(String plainToken) {
        String tokenHash = hash(plainToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!token.isActive()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        return token;
    }

    @Transactional
    public String rotate(String plainToken, HttpServletRequest request) {
        RefreshToken old = validate(plainToken);
        old.setRevokedAt(Instant.now());
        refreshTokenRepository.save(old);
        return generate(old.getUserId(), request);
    }

    @Transactional
    public void revoke(String plainToken) {
        String tokenHash = hash(plainToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
