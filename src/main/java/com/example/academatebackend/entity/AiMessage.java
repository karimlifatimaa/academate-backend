package com.example.academatebackend.entity;

import com.example.academatebackend.enums.AiMessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "ai_messages",
        indexes = @Index(name = "idx_ai_messages_session", columnList = "session_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessage extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private AiMessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "intent", length = 50)
    private String intent;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "feedback")
    private Short feedback;
}
