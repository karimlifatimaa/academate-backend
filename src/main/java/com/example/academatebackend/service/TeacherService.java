package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.*;
import com.example.academatebackend.entity.TeacherAvailability;
import com.example.academatebackend.enums.Subject;
import com.example.academatebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final UserRepository userRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final TeacherAvailabilityRepository availabilityRepository;
    private final ReviewRepository reviewRepository;

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
}
