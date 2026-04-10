package com.example.academatebackend.controller;

import com.example.academatebackend.dto.InviteCodeResponse;
import com.example.academatebackend.dto.LinkChildRequest;
import com.example.academatebackend.dto.UserResponse;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.ParentStudentLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/family")
@RequiredArgsConstructor
public class ParentController {

    private final ParentStudentLinkService linkService;

    // ── Student: generate invite code ─────────────────────────────────────────

    @PostMapping("/invite-code")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<InviteCodeResponse> generateInviteCode() {
        return ResponseEntity.ok(linkService.generateInviteCode(SecurityUtils.requireCurrentUserId()));
    }

    @GetMapping("/my-parents")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<UserResponse>> getMyParents() {
        return ResponseEntity.ok(linkService.getParents(SecurityUtils.requireCurrentUserId()));
    }

    // ── Parent: link to student, list children ────────────────────────────────

    @PostMapping("/link")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<Void> linkChild(@Valid @RequestBody LinkChildRequest req) {
        linkService.linkByInviteCode(SecurityUtils.requireCurrentUserId(), req.getInviteCode());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<UserResponse>> getMyChildren() {
        return ResponseEntity.ok(linkService.getChildren(SecurityUtils.requireCurrentUserId()));
    }
}
