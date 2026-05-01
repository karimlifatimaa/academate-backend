package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.*;
import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.entity.TeacherAvailability;
import com.example.academatebackend.enums.Subject;
import com.example.academatebackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final UserRepository userRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherAvailabilityRepository availabilityRepository;
    private final ReviewRepository reviewRepository;
    private final LessonRepository lessonRepository;

    public Page<TeacherSearchResponse> searchTeachers(Subject subject, Pageable pageable) {
        Page<UUID> teacherIds;
        if (subject != null) {
            teacherIds = teacherSubjectRepository.findTeacherIdsBySubject(subject, pageable);
        } else {
            teacherIds = teacherProfileRepository.findAllTeacherIds(pageable);
        }
        return teacherIds.map(this::buildTeacherSearchResponse);
    }

    public TeacherSearchResponse getTeacherSummary(UUID teacherId) {
        return buildTeacherSearchResponse(teacherId);
    }

    private TeacherSearchResponse buildTeacherSearchResponse(UUID teacherId) {
        var user = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));
        var profile = teacherProfileRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherProfile", teacherId));
        var subjects = teacherSubjectRepository.findByIdTeacherId(teacherId)
                .stream().map(ts -> ts.getId().getSubject()).collect(Collectors.toList());
        Double avgRating = reviewRepository.findAverageRatingByTeacherId(teacherId);
        Long reviewCount = reviewRepository.countByTeacherId(teacherId);

        return TeacherSearchResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(profile.getBio())
                .hourlyRate(profile.getHourlyRate())
                .rating(avgRating)
                .reviewCount(reviewCount)
                .isVerified(profile.getIsVerified())
                .subjects(subjects)
                .build();
    }

    // --- Availability ---

    @Transactional
    public List<AvailabilitySlotResponse> setAvailability(UUID teacherId, List<AvailabilitySlotRequest> slots) {
        log.info("Set availability: teacherId={} slotCount={}", teacherId, slots != null ? slots.size() : 0);
        availabilityRepository.deleteByTeacherId(teacherId);
        List<TeacherAvailability> entities = slots.stream().map(s ->
                TeacherAvailability.builder()
                        .teacherId(teacherId)
                        .dayOfWeek(s.getDayOfWeek())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build()
        ).collect(Collectors.toList());
        availabilityRepository.saveAll(entities);
        log.info("Availability saved: teacherId={} slots={}", teacherId, entities.size());
        return entities.stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    public List<AvailabilitySlotResponse> getAvailability(UUID teacherId) {
        return availabilityRepository.findByTeacherId(teacherId)
                .stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    private AvailabilitySlotResponse toSlotResponse(TeacherAvailability a) {
        return AvailabilitySlotResponse.builder()
                .id(a.getId())
                .dayOfWeek(a.getDayOfWeek())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .build();
    }

    /**
     * Public list of booked time ranges for a teacher within [from, to).
     * Returns only the time windows — no student PII — so it's safe to expose
     * publicly for client-side slot conflict checks.
     */
    public List<BookedTimeResponse> getBookedTimes(UUID teacherId, LocalDateTime from, LocalDateTime to) {
        if (!userRepository.existsById(teacherId)) {
            log.warn("Booked-times requested for non-existent teacher: {}", teacherId);
            throw new ResourceNotFoundException("Teacher", teacherId);
        }
        List<Lesson> lessons = lessonRepository.findActiveBookingsInRange(teacherId, from, to);
        log.debug("Booked times: teacherId={} from={} to={} count={}", teacherId, from, to, lessons.size());
        return lessons.stream().map(l -> BookedTimeResponse.builder()
                .startTime(l.getScheduledAt())
                .endTime(l.getScheduledAt().plusMinutes(
                        l.getDurationMinutes() != null ? l.getDurationMinutes() : 60))
                .build()
        ).collect(Collectors.toList());
    }
}
