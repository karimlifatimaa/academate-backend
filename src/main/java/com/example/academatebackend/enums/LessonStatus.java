package com.example.academatebackend.enums;

public enum LessonStatus {
    PENDING,      // student booked, waiting for teacher confirmation
    CONFIRMED,    // teacher confirmed, meeting link added
    IN_PROGRESS,  // Zoom meeting started
    CANCELLED,    // cancelled by student or teacher
    COMPLETED     // lesson done (Zoom meeting ended)
}
