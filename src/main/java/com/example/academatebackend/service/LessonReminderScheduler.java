package com.example.academatebackend.service;

import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.repository.LessonRepository;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonReminderScheduler {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Hər 5 dəqiqədən bir işləyir
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusMinutes(55);
        LocalDateTime to = LocalDateTime.now().plusMinutes(65);

        List<Lesson> upcoming = lessonRepository.findLessonsForReminder(from, to);
        if (upcoming.isEmpty()) return;

        log.info("{} dərs üçün xatırlatma emaili göndərilir", upcoming.size());

        for (Lesson lesson : upcoming) {
            try {
                User teacher = userRepository.findById(lesson.getTeacherId()).orElse(null);
                User student = userRepository.findById(lesson.getStudentId()).orElse(null);

                if (teacher != null && student != null) {
                    emailService.sendLessonReminderEmail(student, teacher, lesson);
                    emailService.sendLessonReminderEmail(teacher, teacher, lesson);
                }

                lesson.setReminderSent(true);
                lessonRepository.save(lesson);
            } catch (Exception e) {
                log.error("Xatırlatma göndərilərkən xəta (lesson {}): {}", lesson.getId(), e.getMessage());
            }
        }
    }
}
