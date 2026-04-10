package com.example.academatebackend.dto;

import com.example.academatebackend.enums.Subject;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TeacherSearchResponse {
    private UUID id;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private BigDecimal hourlyRate;
    private Double rating;
    private Long reviewCount;
    private Boolean isVerified;
    private List<Subject> subjects;
}
