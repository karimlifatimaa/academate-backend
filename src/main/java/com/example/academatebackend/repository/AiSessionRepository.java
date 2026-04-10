package com.example.academatebackend.repository;

import com.example.academatebackend.entity.AiSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiSessionRepository extends JpaRepository<AiSession, UUID> {

    List<AiSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
