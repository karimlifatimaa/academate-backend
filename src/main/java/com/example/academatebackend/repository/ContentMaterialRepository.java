package com.example.academatebackend.repository;

import com.example.academatebackend.entity.ContentMaterial;
import com.example.academatebackend.enums.ContentStatus;
import com.example.academatebackend.enums.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentMaterialRepository extends JpaRepository<ContentMaterial, UUID> {

    List<ContentMaterial> findByTeacherId(UUID teacherId);

    Page<ContentMaterial> findBySubjectAndGradeAndStatus(Subject subject, Short grade, ContentStatus status, Pageable pageable);
}
