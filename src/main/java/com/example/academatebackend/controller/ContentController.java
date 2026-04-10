package com.example.academatebackend.controller;

import com.example.academatebackend.dto.*;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/public")
    public ResponseEntity<Page<ContentResponse>> listPublic(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(contentService.listPublic(pageable));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ContentResponse> getPublic(@PathVariable UUID id) {
        return ResponseEntity.ok(contentService.getPublic(id));
    }

    // ── Teacher ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ContentResponse> create(
            @Valid @RequestBody CreateContentRequest req) {
        UUID teacherId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.create(teacherId, req));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<ContentResponse>> listMy(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID teacherId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(contentService.listMyContent(teacherId, pageable));
    }

    @PostMapping("/{id}/upload-url")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(
            @PathVariable UUID id,
            @RequestParam String fileName,
            @RequestParam String contentType) {
        UUID teacherId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(contentService.generateUploadUrl(teacherId, id, fileName, contentType));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ContentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContentRequest req) {
        UUID teacherId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(contentService.update(teacherId, id, req));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ContentResponse> publish(@PathVariable UUID id) {
        UUID teacherId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(contentService.publish(teacherId, id));
    }

    // ── Teacher or Admin ──────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contentService.delete(SecurityUtils.requireCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
