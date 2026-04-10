package com.example.academatebackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvatarUploadResponse {
    private String uploadUrl;
    private String avatarUrl;
}
