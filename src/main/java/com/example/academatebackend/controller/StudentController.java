package com.example.academatebackend.controller;

import com.example.academatebackend.dto.*;
import com.example.academatebackend.enums.LessonStatus;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.LessonService;
import com.example.academatebackend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final LessonService lessonService;
    private final ReviewService reviewService;

    @PostMapping("/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LessonResponse> bookLesson(@Valid @RequestBody BookLessonRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.book(SecurityUtils.requireCurrentUserId(), req));
    }

    @GetMapping("/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<LessonResponse>> getMyLessons(
            @RequestParam(required = false) LessonStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lessonService.getStudentLessons(SecurityUtils.requireCurrentUserId(), status, pageable));
    }

    @PostMapping("/lessons/{lessonId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LessonResponse> cancelLesson(
            @PathVariable UUID lessonId,
            @RequestBody(required = false) CancelLessonRequest req) {
        return ResponseEntity.ok(lessonService.cancel(SecurityUtils.requireCurrentUserId(), lessonId, req));
    }

    @PostMapping("/teachers/{teacherId}/reviews")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReviewResponse> leaveReview(
            @PathVariable UUID teacherId,
            @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(SecurityUtils.requireCurrentUserId(), teacherId, req));
    }
}
