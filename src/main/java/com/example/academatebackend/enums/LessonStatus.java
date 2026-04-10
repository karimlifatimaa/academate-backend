package com.example.academatebackend.enums;

public enum LessonStatus {
    PENDING,      // student booked, waiting for teacher confirmation
    CONFIRMED,    // teacher confirmed, meeting link added
    CANCELLED,    // cancelled by student or teacher
    COMPLETED     // lesson done, student can review
}
