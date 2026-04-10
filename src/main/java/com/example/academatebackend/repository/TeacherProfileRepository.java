package com.example.academatebackend.repository;

import com.example.academatebackend.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {
}
