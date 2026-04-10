package com.example.academatebackend.controller;

import com.example.academatebackend.dto.ProgressResponse;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ProgressResponse>> getMyProgress() {
        return ResponseEntity.ok(progressService.getMyProgress(SecurityUtils.requireCurrentUserId()));
    }

    @GetMapping("/child/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<ProgressResponse>> getChildProgress(@PathVariable UUID studentId) {
        return ResponseEntity.ok(
                progressService.getChildProgress(SecurityUtils.requireCurrentUserId(), studentId));
    }
}
