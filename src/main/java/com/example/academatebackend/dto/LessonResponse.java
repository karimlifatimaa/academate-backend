package com.example.academatebackend.dto;

import com.example.academatebackend.enums.LessonStatus;
import com.example.academatebackend.enums.Subject;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class LessonResponse {
    private UUID id;
    private UUID teacherId;
    private String teacherName;
    private String teacherAvatarUrl;
    private UUID studentId;
    private String studentName;
    private Subject subject;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private LessonStatus status;
    private String meetingLink;
    private String notes;
    private String cancellationReason;
    private Instant createdAt;
}
