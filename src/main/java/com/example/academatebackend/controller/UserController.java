package com.example.academatebackend.controller;

import com.example.academatebackend.dto.AvatarUploadResponse;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        return ResponseEntity.ok(
                userService.generateAvatarUploadUrl(
                        SecurityUtils.requireCurrentUserId(), fileName, contentType));
    }
}
