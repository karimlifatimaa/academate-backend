package com.example.academatebackend.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 255)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @Size(max = 5)
    private String preferredLanguage;
}
