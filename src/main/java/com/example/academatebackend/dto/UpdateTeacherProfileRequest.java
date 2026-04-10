package com.example.academatebackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateTeacherProfileRequest {
    private String bio;
    private BigDecimal hourlyRate;
}
