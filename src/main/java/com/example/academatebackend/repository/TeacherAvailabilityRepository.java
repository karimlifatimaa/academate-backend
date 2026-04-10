package com.example.academatebackend.repository;

import com.example.academatebackend.entity.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, UUID> {
    List<TeacherAvailability> findByTeacherId(UUID teacherId);
    void deleteByTeacherId(UUID teacherId);
}
