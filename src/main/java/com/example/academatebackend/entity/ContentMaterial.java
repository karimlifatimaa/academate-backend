package com.example.academatebackend.entity;

import com.example.academatebackend.enums.ContentStatus;
import com.example.academatebackend.enums.ContentType;
import com.example.academatebackend.enums.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "content_materials",
        indexes = {
                @Index(name = "idx_content_teacher", columnList = "teacher_id"),
                @Index(name = "idx_content_subject_grade", columnList = "subject,grade"),
                @Index(name = "idx_content_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentMaterial extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject", nullable = false, length = 30)
    private Subject subject;

    @Column(name = "grade", nullable = false)
    private Short grade;

    @Column(name = "topic", length = 255)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ContentStatus status = ContentStatus.DRAFT;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
}
