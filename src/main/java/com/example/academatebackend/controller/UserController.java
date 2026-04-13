package com.example.academatebackend.controller;

import com.example.academatebackend.dto.*;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyProfileResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile(SecurityUtils.requireCurrentUserId()));
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateMe(SecurityUtils.requireCurrentUserId(), req));
    }

    @PatchMapping("/me/student-profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> updateStudentProfile(@RequestBody UpdateStudentProfileRequest req) {
        userService.updateStudentProfile(SecurityUtils.requireCurrentUserId(), req);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/teacher-profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> updateTeacherProfile(@RequestBody UpdateTeacherProfileRequest req) {
        userService.updateTeacherProfile(SecurityUtils.requireCurrentUserId(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(SecurityUtils.requireCurrentUserId(), file));
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMe() {
        userService.deleteMe(SecurityUtils.requireCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teachers/{id}")
    public ResponseEntity<TeacherPublicProfileResponse> getTeacherProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getTeacherPublicProfile(id));
    }
}
