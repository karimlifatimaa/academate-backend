package com.example.academatebackend.repository;

import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.enums.LessonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    Page<Lesson> findByStudentId(UUID studentId, Pageable pageable);
    Page<Lesson> findByTeacherId(UUID teacherId, Pageable pageable);
    Page<Lesson> findByStudentIdAndStatus(UUID studentId, LessonStatus status, Pageable pageable);
    Page<Lesson> findByTeacherIdAndStatus(UUID teacherId, LessonStatus status, Pageable pageable);
    boolean existsByTeacherIdAndStudentId(UUID teacherId, UUID studentId);

    @Query("SELECT l FROM Lesson l WHERE l.status = 'CONFIRMED' AND l.reminderSent = false AND l.scheduledAt BETWEEN :from AND :to")
    List<Lesson> findLessonsForReminder(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
