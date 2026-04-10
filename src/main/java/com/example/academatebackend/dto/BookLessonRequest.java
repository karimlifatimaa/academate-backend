package com.example.academatebackend.dto;

import com.example.academatebackend.enums.Subject;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BookLessonRequest {
    @NotNull
    private UUID teacherId;
    @NotNull
    private Subject subject;
    @NotNull
    @Future
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String notes;
}
