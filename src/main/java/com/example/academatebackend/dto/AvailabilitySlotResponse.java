package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
public class AvailabilitySlotResponse {
    private UUID id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
