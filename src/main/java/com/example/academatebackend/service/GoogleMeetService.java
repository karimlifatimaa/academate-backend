package com.example.academatebackend.service;

import com.example.academatebackend.config.GoogleCalendarProperties;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMeetService {

    private final GoogleCalendarProperties props;

    public String createMeetLink(String lessonTitle, LocalDateTime scheduledAt, int durationMinutes) {
        try {
            GoogleCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(props.getClientId())
                    .setClientSecret(props.getClientSecret())
                    .setRefreshToken(props.getRefreshToken())
                    .build();

            Calendar calendar = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Academate")
                    .build();

            long startMillis = scheduledAt
                    .atZone(ZoneId.of("Asia/Baku"))
                    .toInstant().toEpochMilli();
            long endMillis = scheduledAt.plusMinutes(durationMinutes)
                    .atZone(ZoneId.of("Asia/Baku"))
                    .toInstant().toEpochMilli();

            Event event = new Event()
                    .setSummary(lessonTitle)
                    .setStart(new EventDateTime().setDateTime(new DateTime(startMillis)))
                    .setEnd(new EventDateTime().setDateTime(new DateTime(endMillis)))
                    .setConferenceData(new ConferenceData()
                            .setCreateRequest(new CreateConferenceRequest()
                                    .setRequestId(UUID.randomUUID().toString())
                                    .setConferenceSolutionKey(
                                            new ConferenceSolutionKey().setType("hangoutsMeet"))));

            Event created = calendar.events()
                    .insert(props.getCalendarId(), event)
                    .setConferenceDataVersion(1)
                    .execute();

            String meetLink = created.getHangoutLink();
            log.info("Google Meet linki yaradıldı: {}", meetLink);
            return meetLink;

        } catch (Exception e) {
            log.error("Google Meet linki yaradılarkən xəta: {}", e.getMessage());
            return null;
        }
    }
}
