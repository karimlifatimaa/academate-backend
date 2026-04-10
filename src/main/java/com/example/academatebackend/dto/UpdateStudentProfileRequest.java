package com.example.academatebackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStudentProfileRequest {
    private Short grade;
    private String schoolName;
    private String city;
}
