package com.example.academatebackend.entity;

import com.example.academatebackend.enums.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "topics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_topic_subject_grade_name",
                columnNames = {"subject", "grade", "name"}
        ),
        indexes = {
                @Index(name = "idx_topic_subject_grade", columnList = "subject,grade")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "subject", nullable = false, length = 30)
    private Subject subject;

    @Column(name = "grade", nullable = false)
    private Short grade;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Short orderIndex = 0;
}
