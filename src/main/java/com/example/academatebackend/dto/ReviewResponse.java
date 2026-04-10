package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentAvatarUrl;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
