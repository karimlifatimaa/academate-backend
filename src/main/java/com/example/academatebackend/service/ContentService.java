package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.ForbiddenException;
import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.*;
import com.example.academatebackend.entity.ContentMaterial;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.enums.ContentStatus;
import com.example.academatebackend.enums.Role;
import com.example.academatebackend.repository.ContentMaterialRepository;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentMaterialRepository contentRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public ContentResponse create(UUID teacherId, CreateContentRequest req) {
        ContentMaterial material = ContentMaterial.builder()
                .teacherId(teacherId)
                .title(req.getTitle())
                .description(req.getDescription())
                .subject(req.getSubject())
                .grade(req.getGrade())
                .topicId(req.getTopicId())
                .contentType(req.getContentType())
                .isFree(req.getIsFree() != null ? req.getIsFree() : true)
                .status(ContentStatus.DRAFT)
                .viewCount(0)
                .build();
        contentRepository.save(material);
        return toResponse(material, teacherId);
    }

    // ── Upload URL ────────────────────────────────────────────────────────────

    @Transactional
    public UploadUrlResponse generateUploadUrl(UUID teacherId, UUID contentId, String fileName, String contentType) {
        ContentMaterial material = findOwned(contentId, teacherId);
        String folder = "content/" + material.getContentType().name().toLowerCase();
        String uploadUrl = s3StorageService.generateUploadUrl(folder, fileName, contentType);
        String key = folder + "/" + contentId + "/" + fileName;

        material.setFileUrl(s3StorageService.getObjectUrl(key));
        contentRepository.save(material);

        return UploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .key(key)
                .build();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public Page<ContentResponse> listPublic(Pageable pageable) {
        return contentRepository.findByStatus(ContentStatus.PUBLISHED, pageable)
                .map(m -> toResponse(m, m.getTeacherId()));
    }

    public ContentResponse getPublic(UUID id) {
        ContentMaterial material = contentRepository.findById(id)
                .filter(m -> m.getStatus() == ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Content", id));
        material.setViewCount(material.getViewCount() + 1);
        contentRepository.save(material);
        return toResponse(material, material.getTeacherId());
    }

    public Page<ContentResponse> listMyContent(UUID teacherId, Pageable pageable) {
        return contentRepository.findByTeacherId(teacherId, pageable)
                .map(m -> toResponse(m, teacherId));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public ContentResponse update(UUID requesterId, UUID contentId, UpdateContentRequest req) {
        ContentMaterial material = findOwned(contentId, requesterId);

        if (req.getTitle() != null) material.setTitle(req.getTitle());
        if (req.getDescription() != null) material.setDescription(req.getDescription());
        if (req.getIsFree() != null) material.setIsFree(req.getIsFree());
        if (req.getStatus() != null) material.setStatus(req.getStatus());

        contentRepository.save(material);
        return toResponse(material, requesterId);
    }

    @Transactional
    public ContentResponse publish(UUID teacherId, UUID contentId) {
        ContentMaterial material = findOwned(contentId, teacherId);
        material.setStatus(ContentStatus.PUBLISHED);
        contentRepository.save(material);
        return toResponse(material, teacherId);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID requesterId, UUID contentId) {
        ContentMaterial material = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        Role requesterRole = userRepository.findById(requesterId)
                .map(User::getRole)
                .orElse(null);

        if (requesterRole != Role.ADMIN && !material.getTeacherId().equals(requesterId)) {
            throw new ForbiddenException("You do not own this content");
        }

        if (material.getFileUrl() != null) {
            try {
                s3StorageService.delete(extractKey(material.getFileUrl()));
            } catch (Exception ignored) {}
        }

        contentRepository.delete(material);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ContentMaterial findOwned(UUID contentId, UUID teacherId) {
        ContentMaterial material = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        if (!material.getTeacherId().equals(teacherId)) {
            throw new ForbiddenException("You do not own this content");
        }
        return material;
    }

    private ContentResponse toResponse(ContentMaterial m, UUID teacherId) {
        String teacherName = userRepository.findById(teacherId)
                .map(User::getFullName).orElse(null);
        return ContentResponse.builder()
                .id(m.getId())
                .teacherId(m.getTeacherId())
                .teacherName(teacherName)
                .title(m.getTitle())
                .description(m.getDescription())
                .subject(m.getSubject())
                .grade(m.getGrade())
                .topicId(m.getTopicId())
                .contentType(m.getContentType())
                .fileUrl(m.getFileUrl())
                .fileSizeBytes(m.getFileSizeBytes())
                .thumbnailUrl(m.getThumbnailUrl())
                .durationSec(m.getDurationSec())
                .isFree(m.getIsFree())
                .status(m.getStatus())
                .viewCount(m.getViewCount())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private String extractKey(String url) {
        int idx = url.indexOf(".amazonaws.com/");
        return idx >= 0 ? url.substring(idx + 15) : url;
    }
}
