package com.example.academatebackend.dto;

import com.example.academatebackend.enums.ContentStatus;
import com.example.academatebackend.enums.ContentType;
import com.example.academatebackend.enums.Subject;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ContentResponse {
    private UUID id;
    private UUID teacherId;
    private String teacherName;
    private String title;
    private String description;
    private Subject subject;
    private Short grade;
    private UUID topicId;
    private ContentType contentType;
    private String fileUrl;
    private Long fileSizeBytes;
    private String thumbnailUrl;
    private Integer durationSec;
    private Boolean isFree;
    private ContentStatus status;
    private Integer viewCount;
    private Instant createdAt;
}
