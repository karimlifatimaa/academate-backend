package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.BadRequestException;
import com.example.academatebackend.common.exception.ForbiddenException;
import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.*;
import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.enums.LessonStatus;
import com.example.academatebackend.repository.LessonRepository;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final GoogleMeetService googleMeetService;
    private final EmailService emailService;

    @Transactional
    public LessonResponse book(UUID studentId, BookLessonRequest req) {
        userRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", req.getTeacherId()));

        Lesson lesson = Lesson.builder()
                .teacherId(req.getTeacherId())
                .studentId(studentId)
                .subject(req.getSubject())
                .scheduledAt(req.getScheduledAt())
                .durationMinutes(req.getDurationMinutes() != null ? req.getDurationMinutes() : 60)
                .notes(req.getNotes())
                .status(LessonStatus.PENDING)
                .build();

        return toLessonResponse(lessonRepository.save(lesson));
    }

    public Page<LessonResponse> getStudentLessons(UUID studentId, LessonStatus status, Pageable pageable) {
        Page<Lesson> lessons = status != null
                ? lessonRepository.findByStudentIdAndStatus(studentId, status, pageable)
                : lessonRepository.findByStudentId(studentId, pageable);
        return lessons.map(this::toLessonResponse);
    }

    public Page<LessonResponse> getTeacherLessons(UUID teacherId, LessonStatus status, Pageable pageable) {
        Page<Lesson> lessons = status != null
                ? lessonRepository.findByTeacherIdAndStatus(teacherId, status, pageable)
                : lessonRepository.findByTeacherId(teacherId, pageable);
        return lessons.map(this::toLessonResponse);
    }

    @Transactional
    public LessonResponse confirm(UUID teacherId, UUID lessonId) {
        Lesson lesson = getLesson(lessonId);
        if (!lesson.getTeacherId().equals(teacherId)) throw new ForbiddenException("Bu dərs sizin deyil");
        if (lesson.getStatus() != LessonStatus.PENDING) throw new BadRequestException("Yalnız PENDING dərslər təsdiqlənə bilər");

        User teacher = userRepository.findById(teacherId).orElseThrow();
        User student = userRepository.findById(lesson.getStudentId()).orElseThrow();

        // Auto-generate Google Meet link
        String meetLink = googleMeetService.createMeetLink(
                "Academate — " + lesson.getSubject() + " dərsi",
                lesson.getScheduledAt(),
                lesson.getDurationMinutes());
        if (meetLink == null) {
            log.warn("Google Meet linki yaradıla bilmədi, dərs ID: {}", lessonId);
        }

        lesson.setStatus(LessonStatus.CONFIRMED);
        lesson.setMeetingLink(meetLink);
        lessonRepository.save(lesson);

        // Send confirmation emails
        emailService.sendLessonConfirmationEmail(student, teacher, lesson, meetLink);
        emailService.sendLessonConfirmationEmail(teacher, teacher, lesson, meetLink);

        return toLessonResponse(lesson);
    }

    @Transactional
    public LessonResponse cancel(UUID userId, UUID lessonId, CancelLessonRequest req) {
        Lesson lesson = getLesson(lessonId);
        boolean isTeacher = lesson.getTeacherId().equals(userId);
        boolean isStudent = lesson.getStudentId().equals(userId);
        if (!isTeacher && !isStudent) throw new ForbiddenException("Bu dərs sizin deyil");
        if (lesson.getStatus() == LessonStatus.COMPLETED || lesson.getStatus() == LessonStatus.CANCELLED) {
            throw new BadRequestException("Bu dərs artıq " + lesson.getStatus() + " statusundadır");
        }

        lesson.setStatus(LessonStatus.CANCELLED);
        lesson.setCancellationReason(req != null ? req.getReason() : null);
        return toLessonResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse complete(UUID teacherId, UUID lessonId) {
        Lesson lesson = getLesson(lessonId);
        if (!lesson.getTeacherId().equals(teacherId)) throw new ForbiddenException("Bu dərs sizin deyil");
        if (lesson.getStatus() != LessonStatus.CONFIRMED) throw new BadRequestException("Yalnız CONFIRMED dərslər tamamlana bilər");

        lesson.setStatus(LessonStatus.COMPLETED);
        return toLessonResponse(lessonRepository.save(lesson));
    }

    private Lesson getLesson(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
    }

    private LessonResponse toLessonResponse(Lesson l) {
        User teacher = userRepository.findById(l.getTeacherId()).orElse(null);
        User student = userRepository.findById(l.getStudentId()).orElse(null);

        return LessonResponse.builder()
                .id(l.getId())
                .teacherId(l.getTeacherId())
                .teacherName(teacher != null ? teacher.getFullName() : null)
                .teacherAvatarUrl(teacher != null ? teacher.getAvatarUrl() : null)
                .studentId(l.getStudentId())
                .studentName(student != null ? student.getFullName() : null)
                .subject(l.getSubject())
                .scheduledAt(l.getScheduledAt())
                .durationMinutes(l.getDurationMinutes())
                .status(l.getStatus())
                .meetingLink(l.getMeetingLink())
                .notes(l.getNotes())
                .cancellationReason(l.getCancellationReason())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
