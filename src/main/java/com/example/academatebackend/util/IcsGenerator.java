package com.example.academatebackend.util;

import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.entity.User;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class IcsGenerator {

    private static final DateTimeFormatter ICS_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public static String generate(Lesson lesson, User teacher, String meetLink) {
        String start = lesson.getScheduledAt()
                .toInstant(ZoneOffset.UTC)
                .atOffset(ZoneOffset.UTC)
                .format(ICS_FORMAT);

        String end = lesson.getScheduledAt()
                .plusMinutes(lesson.getDurationMinutes())
                .toInstant(ZoneOffset.UTC)
                .atOffset(ZoneOffset.UTC)
                .format(ICS_FORMAT);

        String location = meetLink != null ? meetLink : "Academate";
        String description = "Müəllim: " + teacher.getFullName() +
                (meetLink != null ? "\\nKeçid linki: " + meetLink : "");

        return """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//Academate//Academate//AZ
                METHOD:REQUEST
                BEGIN:VEVENT
                UID:%s@academate.az
                DTSTART:%s
                DTEND:%s
                SUMMARY:Academate Dərsi — %s
                DESCRIPTION:%s
                LOCATION:%s
                URL:%s
                STATUS:CONFIRMED
                END:VEVENT
                END:VCALENDAR
                """.formatted(
                lesson.getId(),
                start,
                end,
                lesson.getSubject(),
                description,
                location,
                location
        );
    }
}
