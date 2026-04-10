package com.example.academatebackend.dto;

import com.example.academatebackend.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @StrongPassword
    private String newPassword;
}
