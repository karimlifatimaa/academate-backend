package com.example.academatebackend.service;

import com.example.academatebackend.config.ZoomProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomService {

    private final ZoomProperties zoomProperties;
    private final ObjectMapper objectMapper;

    private static final String TOKEN_URL = "https://zoom.us/oauth/token?grant_type=account_credentials&account_id=";
    private static final String MEETINGS_URL = "https://api.zoom.us/v2/users/me/meetings";

    public String createMeeting(String topic, LocalDateTime scheduledAt, int durationMinutes) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) return null;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String startTime = scheduledAt
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

            Map<String, Object> body = Map.of(
                    "topic", topic,
                    "type", 2,
                    "start_time", startTime,
                    "duration", durationMinutes,
                    "timezone", "Asia/Baku",
                    "settings", Map.of(
                            "join_before_host", true,
                            "waiting_room", false
                    )
            );

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    MEETINGS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode json = objectMapper.readTree(response.getBody());
            String joinUrl = json.get("join_url").asText();
            log.info("Zoom meeting yaradıldı: {}", joinUrl);
            return joinUrl;

        } catch (Exception e) {
            log.error("Zoom meeting yaradılarkən xəta: {}", e.getMessage());
            return null;
        }
    }

    private String getAccessToken() {
        try {
            String credentials = zoomProperties.getClientId() + ":" + zoomProperties.getClientSecret();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encoded);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    TOKEN_URL + zoomProperties.getAccountId(),
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    String.class
            );

            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("access_token").asText();

        } catch (Exception e) {
            log.error("Zoom token alınarkən xəta: {}", e.getMessage());
            return null;
        }
    }
}
