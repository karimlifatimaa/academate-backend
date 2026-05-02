package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalStudents;
    private long totalTeachers;
    private long totalParents;
    private long verifiedTeachers;
    private long pendingTeachers;
}
