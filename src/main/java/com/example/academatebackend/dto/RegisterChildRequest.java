package com.example.academatebackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// Valideyn tərəfindən 5-12 yaş uşaq üçün hesab yaradılır (email/şifrə olmur)
@Getter
@Setter
public class RegisterChildRequest {

    @NotBlank
    private String fullName;

    @NotNull
    @Min(1) @Max(11)
    private Short grade;

    private String schoolName;
    private String city;

    @NotNull
    @Past
    private LocalDate birthDate;
}
