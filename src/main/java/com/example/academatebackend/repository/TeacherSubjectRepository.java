package com.example.academatebackend.repository;

import com.example.academatebackend.entity.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, TeacherSubject.TeacherSubjectId> {

    List<TeacherSubject> findByIdTeacherId(UUID teacherId);
}
