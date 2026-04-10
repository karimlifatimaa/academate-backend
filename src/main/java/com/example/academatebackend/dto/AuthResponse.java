package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn; // saniyə
    private UserResponse user;
}
