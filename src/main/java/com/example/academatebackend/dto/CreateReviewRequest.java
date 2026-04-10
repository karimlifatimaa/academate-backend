package com.example.academatebackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class CreateReviewRequest {
    @NotNull
    private UUID lessonId;
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
    private String comment;
}
