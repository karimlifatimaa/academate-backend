package com.example.academatebackend.controller;

import com.example.academatebackend.config.ZoomProperties;
import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.enums.LessonStatus;
import com.example.academatebackend.repository.LessonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/zoom")
@RequiredArgsConstructor
public class ZoomWebhookController {

    private final ZoomProperties zoomProperties;
    private final LessonRepository lessonRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> handle(
            @RequestHeader(value = "x-zm-signature", required = false) String signature,
            @RequestHeader(value = "x-zm-request-timestamp", required = false) String timestamp,
            @RequestBody String rawBody) {

        // 1. Signature doğrulama
        if (!isValidSignature(signature, timestamp, rawBody)) {
            log.warn("Zoom webhook: etibarsız signature rədd edildi");
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            log.info("Zoom webhook alındı: event={}", event);

            // 2. URL doğrulama (Zoom Challenge-Response)
            if ("endpoint.url_validation".equals(event)) {
                return handleUrlValidation(root);
            }

            String meetingId = root.path("payload").path("object").path("id").asText(null);
            if (meetingId == null || meetingId.isBlank()) {
                log.warn("Zoom webhook: meetingId yoxdur, event={}", event);
                return ResponseEntity.ok().build();
            }

            // 3. Dərsi tap
            Optional<Lesson> lessonOpt = lessonRepository.findByZoomMeetingId(meetingId);
            if (lessonOpt.isEmpty()) {
                log.warn("Zoom webhook: meetingId={} üçün dərs tapılmadı, event={}", meetingId, event);
                return ResponseEntity.ok().build();
            }
            Lesson lesson = lessonOpt.get();

            // 4. Statusa görə yenilə
            switch (event) {
                case "meeting.started" -> {
                    if (lesson.getStatus() == LessonStatus.CONFIRMED) {
                        lesson.setStatus(LessonStatus.IN_PROGRESS);
                        lessonRepository.save(lesson);
                        log.info("Dərs başladı: lessonId={} meetingId={}", lesson.getId(), meetingId);
                    }
                }
                case "meeting.ended" -> {
                    if (lesson.getStatus() == LessonStatus.IN_PROGRESS
                            || lesson.getStatus() == LessonStatus.CONFIRMED) {
                        lesson.setStatus(LessonStatus.COMPLETED);
                        lessonRepository.save(lesson);
                        log.info("Dərs tamamlandı: lessonId={} meetingId={}", lesson.getId(), meetingId);
                    }
                }
                default -> log.debug("Zoom webhook: tanınmamış event={}", event);
            }

        } catch (Exception e) {
            log.error("Zoom webhook emal xətası: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> handleUrlValidation(JsonNode root) {
        try {
            String plainToken = root.path("payload").path("plainToken").asText();
            String encryptedToken = hmacSha256(zoomProperties.getWebhookSecretToken(), plainToken);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("plainToken", plainToken);
            response.put("encryptedToken", encryptedToken);

            log.info("Zoom URL doğrulaması uğurlu tamamlandı");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Zoom URL doğrulama xətası: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isValidSignature(String signature, String timestamp, String body) {
        if (signature == null || timestamp == null || body == null) return false;
        try {
            String message = "v0:" + timestamp + ":" + body;
            String computed = "v0=" + hmacSha256(zoomProperties.getWebhookSecretToken(), message);
            return MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Signature yoxlama xətası: {}", e.getMessage());
            return false;
        }
    }

    private String hmacSha256(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
