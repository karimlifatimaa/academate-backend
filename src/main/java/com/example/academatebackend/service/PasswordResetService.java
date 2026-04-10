package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.BadRequestException;
import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.entity.PasswordResetToken;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.repository.PasswordResetTokenRepository;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.password-reset.token-expiration}")
    private Duration tokenExpiration;

    @Transactional
    public void sendResetEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String plainToken = UUID.randomUUID().toString();
        String tokenHash = hash(plainToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(tokenExpiration))
                .build();

        tokenRepository.save(token);
        emailService.sendPasswordResetEmail(user, plainToken);
    }

    @Transactional
    public void reset(String plainToken, String newPassword) {
        String tokenHash = hash(plainToken);

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (!token.isUsable()) {
            throw new BadRequestException("Reset token has expired or already been used");
        }

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts((short) 0);
        user.setLockedUntil(null);
        userRepository.save(user);
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
}
