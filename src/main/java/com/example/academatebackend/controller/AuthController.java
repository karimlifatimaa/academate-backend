package com.example.academatebackend.controller;

import com.example.academatebackend.dto.*;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.AuthService;
import com.example.academatebackend.service.EmailVerificationService;
import com.example.academatebackend.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    // ── Register ──────────────────────────────────────────────────────────────

    @PostMapping("/register/student")
    public ResponseEntity<AuthResponse> registerStudent(
            @Valid @RequestBody RegisterStudentRequest req,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(req, request));
    }

    @PostMapping("/register/teacher")
    public ResponseEntity<AuthResponse> registerTeacher(
            @Valid @RequestBody RegisterTeacherRequest req,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTeacher(req, request));
    }

    @PostMapping("/register/parent")
    public ResponseEntity<AuthResponse> registerParent(
            @Valid @RequestBody RegisterParentRequest req,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerParent(req, request));
    }

    @PostMapping("/register/child")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<UserResponse> registerChild(
            @Valid @RequestBody RegisterChildRequest req,
            HttpServletRequest request) {
        UUID parentId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerChild(req, parentId, request));
    }

    // ── Login / Token ─────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(authService.login(req, request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(authService.refresh(req, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest req) {
        authService.logout(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logoutAll() {
        authService.logoutAll(SecurityUtils.requireCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    // ── Email Verification ────────────────────────────────────────────────────

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resendVerification() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        authService.resendVerificationEmail(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Password ──────────────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.sendResetEmail(req.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.reset(req.getToken(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(SecurityUtils.requireCurrentUserId(), req);
        return ResponseEntity.noContent().build();
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.getMe(SecurityUtils.requireCurrentUserId()));
    }
}
