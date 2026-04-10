package com.example.academatebackend.entity;

import com.example.academatebackend.enums.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "student_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_progress_student_subject_topic",
                columnNames = {"student_id", "subject", "topic"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProgress extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject", nullable = false, length = 30)
    private Subject subject;

    @Column(name = "topic", nullable = false, length = 255)
    private String topic;

    @Column(name = "mastery_score", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal masteryScore = BigDecimal.ZERO;

    @Column(name = "questions_asked", nullable = false)
    @Builder.Default
    private Integer questionsAsked = 0;

    @Column(name = "correct_answers", nullable = false)
    @Builder.Default
    private Integer correctAnswers = 0;

    @Column(name = "last_studied_at")
    private Instant lastStudiedAt;
}
