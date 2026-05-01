package com.example.academatebackend.controller;

import com.example.academatebackend.dto.*;
import com.example.academatebackend.enums.LessonStatus;
import com.example.academatebackend.enums.Subject;
import com.example.academatebackend.security.SecurityUtils;
import com.example.academatebackend.service.LessonService;
import com.example.academatebackend.service.ReviewService;
import com.example.academatebackend.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final LessonService lessonService;
    private final ReviewService reviewService;

    // --- Public ---

    @GetMapping
    public ResponseEntity<Page<TeacherSearchResponse>> search(
            @RequestParam(required = false) Subject subject,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(teacherService.searchTeachers(subject, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherSearchResponse> getTeacher(@PathVariable UUID id) {
        return ResponseEntity.ok(teacherService.getTeacherSummary(id));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<AvailabilitySlotResponse>> getAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(teacherService.getAvailability(id));
    }

    /**
     * Booked time ranges for a teacher within [from, to). Public — returns only
     * start/end timestamps (no student PII) so the booking UI can show which slots
     * are taken. If `from`/`to` are omitted, defaults to the next 14 days.
     */
    @GetMapping("/{id}/booked-times")
    public ResponseEntity<List<BookedTimeResponse>> getBookedTimes(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate fromDate = from != null ? from : LocalDate.now();
        LocalDate toDate = to != null ? to : fromDate.plusDays(14);
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.atStartOfDay();
        return ResponseEntity.ok(teacherService.getBookedTimes(id, fromDt, toDt));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable UUID id,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getTeacherReviews(id, pageable));
    }

    // --- Teacher only ---

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AvailabilitySlotResponse>> setAvailability(
            @RequestBody List<AvailabilitySlotRequest> slots) {
        return ResponseEntity.ok(teacherService.setAvailability(SecurityUtils.requireCurrentUserId(), slots));
    }

    @GetMapping("/me/lessons")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<LessonResponse>> getMyLessons(
            @RequestParam(required = false) LessonStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lessonService.getTeacherLessons(SecurityUtils.requireCurrentUserId(), status, pageable));
    }

    @PostMapping("/me/lessons/{lessonId}/confirm")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> confirmLesson(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(lessonService.confirm(SecurityUtils.requireCurrentUserId(), lessonId));
    }

    @PostMapping("/me/lessons/{lessonId}/complete")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> completeLesson(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(lessonService.complete(SecurityUtils.requireCurrentUserId(), lessonId));
    }

    @PostMapping("/me/lessons/{lessonId}/cancel")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> cancelLesson(
            @PathVariable UUID lessonId,
            @RequestBody(required = false) CancelLessonRequest req) {
        return ResponseEntity.ok(lessonService.cancel(SecurityUtils.requireCurrentUserId(), lessonId, req));
    }
}
