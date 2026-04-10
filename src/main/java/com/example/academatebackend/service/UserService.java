package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.AvatarUploadResponse;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    @Transactional
    public AvatarUploadResponse generateAvatarUploadUrl(UUID userId, String fileName, String contentType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String uploadUrl = s3StorageService.generateUploadUrl("avatars", fileName, contentType);

        // Key formatı: avatars/{uuid}.{ext}
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
        String key = "avatars/" + userId + ext;
        String avatarUrl = s3StorageService.getObjectUrl(key);

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return AvatarUploadResponse.builder()
                .uploadUrl(uploadUrl)
                .avatarUrl(avatarUrl)
                .build();
    }
}
