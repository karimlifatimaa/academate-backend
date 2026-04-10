package com.example.academatebackend.repository;

import com.example.academatebackend.entity.StudentProgress;
import com.example.academatebackend.enums.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProgressRepository extends JpaRepository<StudentProgress, UUID> {

    List<StudentProgress> findByStudentId(UUID studentId);

    Optional<StudentProgress> findByStudentIdAndSubjectAndTopic(UUID studentId, Subject subject, String topic);
}
