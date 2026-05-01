package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.BadRequestException;
import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.TeacherSummaryResponse;
import com.example.academatebackend.dto.UserResponse;
import com.example.academatebackend.entity.TeacherProfile;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.enums.Role;
import com.example.academatebackend.repository.TeacherProfileRepository;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    public Page<TeacherSummaryResponse> listTeachers(Pageable pageable) {
        return userRepository.findByRole(Role.TEACHER, pageable)
                .map(user -> {
                    TeacherProfile profile = teacherProfileRepository.findById(user.getId()).orElse(null);
                    return TeacherSummaryResponse.builder()
                            .userId(user.getId())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .isVerified(profile != null && Boolean.TRUE.equals(profile.getIsVerified()))
                            .verifiedAt(profile != null ? profile.getVerifiedAt() : null)
                            .build();
                });
    }

    @Transactional
    public TeacherSummaryResponse verifyTeacher(UUID adminId, UUID teacherId) {
        log.info("Verify teacher: adminId={} teacherId={}", adminId, teacherId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        if (teacher.getRole() != Role.TEACHER) {
            log.warn("Verify teacher rejected - not a teacher: userId={} role={}", teacherId, teacher.getRole());
            throw new BadRequestException("User is not a teacher");
        }

        TeacherProfile profile = teacherProfileRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherProfile", teacherId));

        profile.setIsVerified(true);
        profile.setVerifiedAt(Instant.now());
        profile.setVerifiedBy(adminId);
        teacherProfileRepository.save(profile);

        log.info("Teacher verified: teacherId={} by adminId={}", teacherId, adminId);
        return TeacherSummaryResponse.builder()
                .userId(teacher.getId())
                .fullName(teacher.getFullName())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .isVerified(true)
                .verifiedAt(profile.getVerifiedAt())
                .build();
    }

    @Transactional
    public void deactivateUser(UUID userId) {
        log.info("Deactivate user: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: userId={} email={}", userId, user.getEmail());
    }

    public Page<UserResponse> listUsers(Role role, Pageable pageable) {
        Page<User> users = role != null
                ? userRepository.findByRole(role, pageable)
                : userRepository.findAll(pageable);
        return users.map(this::toUserResponse);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}
