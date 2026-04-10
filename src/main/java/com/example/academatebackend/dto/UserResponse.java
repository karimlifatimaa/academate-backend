package com.example.academatebackend.dto;

import com.example.academatebackend.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String username;
    private Role role;
    private String avatarUrl;
    private boolean emailVerified;
}
