package com.example.academatebackend.dto;

import com.example.academatebackend.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterStudentRequest {

    @NotBlank
    private String fullName;

    // 13+ yaş üçün məcburi, uşaq üçün null ola bilər
    @Email
    private String email;

    @StrongPassword
    private String password;

    @NotNull
    @Min(1) @Max(11)
    private Short grade;

    private String schoolName;
    private String city;

    @NotNull
    @Past
    private LocalDate birthDate;
}
