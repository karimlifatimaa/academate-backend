package com.example.academatebackend.dto;

import com.example.academatebackend.enums.Subject;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ProgressResponse {
    private UUID id;
    private UUID studentId;
    private UUID topicId;
    private Subject subject;
    private BigDecimal masteryScore;
    private Integer questionsAsked;
    private Integer correctAnswers;
    private Instant lastStudiedAt;
}
