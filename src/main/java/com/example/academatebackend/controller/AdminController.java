package com.example.academatebackend.controller;

import com.example.academatebackend.dto.TeacherSummaryResponse;
import com.example.academatebackend.dto.UserResponse;
import com.example.academatebackend.enums.Role;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listUsers(role, pageable));
    }

    @GetMapping("/teachers")
    public ResponseEntity<Page<TeacherSummaryResponse>> listTeachers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listTeachers(pageable));
    }

    @PatchMapping("/teachers/{teacherId}/verify")
    public ResponseEntity<TeacherSummaryResponse> verifyTeacher(@PathVariable UUID teacherId) {
        return ResponseEntity.ok(adminService.verifyTeacher(SecurityUtils.requireCurrentUserId(), teacherId));
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
        adminService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
