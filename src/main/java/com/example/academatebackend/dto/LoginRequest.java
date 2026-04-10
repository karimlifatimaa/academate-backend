package com.example.academatebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    private String identifier; // email və ya username

    @NotBlank
    private String password;
}
