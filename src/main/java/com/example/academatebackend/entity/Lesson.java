package com.example.academatebackend.entity;

import com.example.academatebackend.enums.LessonStatus;
import com.example.academatebackend.enums.Subject;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends BaseEntity {

    @Column(nullable = false)
    private UUID teacherId;

    @Column(nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Subject subject;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Builder.Default
    private Integer durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LessonStatus status = LessonStatus.PENDING;

    private String meetingLink;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String cancellationReason;

    @Builder.Default
    private Boolean reminderSent = false;
}
