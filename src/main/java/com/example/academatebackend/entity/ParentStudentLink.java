package com.example.academatebackend.entity;

import com.example.academatebackend.enums.LinkCreatedVia;
import com.example.academatebackend.enums.RelationType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parent_student_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentStudentLink {

    @EmbeddedId
    private ParentStudentLinkId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation", nullable = false, length = 20)
    private RelationType relation;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_via", nullable = false, length = 20)
    private LinkCreatedVia createdVia;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Embeddable
    public static class ParentStudentLinkId implements Serializable {

        @Column(name = "parent_id", nullable = false)
        private UUID parentId;

        @Column(name = "student_id", nullable = false)
        private UUID studentId;
    }
}
