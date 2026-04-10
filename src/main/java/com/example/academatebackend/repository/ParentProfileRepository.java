package com.example.academatebackend.repository;

import com.example.academatebackend.entity.ParentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ParentProfileRepository extends JpaRepository<ParentProfile, UUID> {
}
