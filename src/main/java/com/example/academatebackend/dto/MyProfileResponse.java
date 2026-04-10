package com.example.academatebackend.dto;

import com.example.academatebackend.enums.Role;
import com.example.academatebackend.enums.Subject;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MyProfileResponse {
    // User
    private UUID id;
    private String fullName;
    private String email;
    private String username;
    private String phone;
    private String avatarUrl;
    private String preferredLanguage;
    private Role role;
    private boolean emailVerified;

    // Student
    private Short grade;
    private String schoolName;
    private String city;
    private LocalDate birthDate;

    // Teacher
    private String bio;
    private BigDecimal hourlyRate;
    private BigDecimal rating;
    private Boolean isVerified;
    private List<Subject> subjects;

    // Parent
    private String occupation;
}
