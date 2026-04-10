package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TeacherSummaryResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private Boolean isVerified;
    private Instant verifiedAt;
}
