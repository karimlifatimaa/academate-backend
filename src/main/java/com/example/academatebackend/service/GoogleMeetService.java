package com.example.academatebackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class GoogleMeetService {

    public String createMeetLink(String lessonTitle, LocalDateTime scheduledAt, int durationMinutes) {
        String roomName = "academate-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String link = "https://meet.jit.si/" + roomName;
        log.info("Jitsi Meet linki yaradıldı: {}", link);
        return link;
    }
}
