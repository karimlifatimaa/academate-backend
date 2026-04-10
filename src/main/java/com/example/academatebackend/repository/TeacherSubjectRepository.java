package com.example.academatebackend.repository;

import com.example.academatebackend.entity.TeacherSubject;
import com.example.academatebackend.enums.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, TeacherSubject.TeacherSubjectId> {

    List<TeacherSubject> findByIdTeacherId(UUID teacherId);

    @Query("SELECT DISTINCT ts.id.teacherId FROM TeacherSubject ts WHERE ts.id.subject = :subject")
    Page<UUID> findTeacherIdsBySubject(@Param("subject") Subject subject, Pageable pageable);
}
