package com.example.academatebackend.repository;

import com.example.academatebackend.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    List<AiMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
