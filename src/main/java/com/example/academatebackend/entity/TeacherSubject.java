package com.example.academatebackend.entity;

import com.example.academatebackend.enums.Subject;
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
import java.util.UUID;

@Entity
@Table(name = "teacher_subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSubject {

    @EmbeddedId
    private TeacherSubjectId id;

    @Column(name = "grade_min")
    private Short gradeMin;

    @Column(name = "grade_max")
    private Short gradeMax;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Embeddable
    public static class TeacherSubjectId implements Serializable {

        @Column(name = "teacher_id", nullable = false)
        private UUID teacherId;

        @Enumerated(EnumType.STRING)
        @Column(name = "subject", nullable = false, length = 30)
        private Subject subject;
    }
}
