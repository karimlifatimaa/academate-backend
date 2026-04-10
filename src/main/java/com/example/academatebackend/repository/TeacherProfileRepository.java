package com.example.academatebackend.repository;

import com.example.academatebackend.entity.TeacherProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {

    @Query("SELECT tp.id FROM TeacherProfile tp")
    Page<UUID> findAllTeacherIds(Pageable pageable);
}
