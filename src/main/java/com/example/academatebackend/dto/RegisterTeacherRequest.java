package com.example.academatebackend.dto;

import com.example.academatebackend.common.validation.StrongPassword;
import com.example.academatebackend.enums.Subject;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterTeacherRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @StrongPassword
    private String password;

    private String phone;

    @NotNull
    @NotEmpty
    private List<Subject> subjects;
}
