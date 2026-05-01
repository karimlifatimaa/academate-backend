package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookedTimeResponse {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
