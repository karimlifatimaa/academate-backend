package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class InviteCodeResponse {
    private String code;
    private Instant expiresAt;
}
