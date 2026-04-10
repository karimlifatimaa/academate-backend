package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.BadRequestException;
import com.example.academatebackend.entity.EmailVerificationToken;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.repository.EmailVerificationTokenRepository;
import com.example.academatebackend.repository.UserRepository;
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
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.security.email-verification.token-expiration}")
    private Duration tokenExpiration;

    @Transactional
    public void sendVerificationEmail(User user) {
        String plainToken = UUID.randomUUID().toString();
        String tokenHash = hash(plainToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(tokenExpiration))
                .build();

        tokenRepository.save(token);
        emailService.sendVerificationEmail(user, plainToken);
    }

    @Transactional
    public void verify(String plainToken) {
        String tokenHash = hash(plainToken);

        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (!token.isUsable()) {
            throw new BadRequestException("Verification token has expired or already been used");
        }

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setEmailVerifiedAt(Instant.now());
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
