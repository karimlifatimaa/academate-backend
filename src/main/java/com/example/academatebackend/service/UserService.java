package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.*;
import com.example.academatebackend.entity.*;
import com.example.academatebackend.enums.Subject;
import com.example.academatebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final S3StorageService s3StorageService;

    // ── Avatar ────────────────────────────────────────────────────────────────

    @Transactional
    public AvatarUploadResponse uploadAvatar(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        try {
            // Köhnə avatarı S3-dən sil
            if (user.getAvatarUrl() != null) {
                String oldKey = extractKeyFromUrl(user.getAvatarUrl());
                if (oldKey != null) s3StorageService.delete(oldKey);
            }

            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String avatarUrl = s3StorageService.uploadFile(
                    "avatars", file.getOriginalFilename(), contentType, file.getBytes());

            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return AvatarUploadResponse.builder()
                    .avatarUrl(avatarUrl)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Avatar yüklənərkən xəta baş verdi: " + e.getMessage());
        }
    }

    // ── My profile ────────────────────────────────────────────────────────────

    public MyProfileResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        MyProfileResponse.MyProfileResponseBuilder builder = MyProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .preferredLanguage(user.getPreferredLanguage())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified());

        switch (user.getRole()) {
            case STUDENT -> studentProfileRepository.findById(userId).ifPresent(p -> builder
                    .grade(p.getGrade())
                    .schoolName(p.getSchoolName())
                    .city(p.getCity())
                    .birthDate(p.getBirthDate()));
            case TEACHER -> teacherProfileRepository.findById(userId).ifPresent(p -> {
                List<Subject> subjects = teacherSubjectRepository.findByIdTeacherId(userId)
                        .stream().map(ts -> ts.getId().getSubject()).collect(Collectors.toList());
                builder.bio(p.getBio())
                        .hourlyRate(p.getHourlyRate())
                        .rating(p.getRating())
                        .isVerified(p.getIsVerified())
                        .subjects(subjects);
            });
            case PARENT -> parentProfileRepository.findById(userId).ifPresent(p ->
                    builder.occupation(p.getOccupation()));
            default -> {}
        }

        return builder.build();
    }

    // ── Update user ───────────────────────────────────────────────────────────

    @Transactional
    public UserResponse updateMe(UUID userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getPreferredLanguage() != null) user.setPreferredLanguage(req.getPreferredLanguage());
        userRepository.save(user);

        return toUserResponse(user);
    }

    // ── Update student profile ────────────────────────────────────────────────

    @Transactional
    public void updateStudentProfile(UUID userId, UpdateStudentProfileRequest req) {
        StudentProfile profile = studentProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentProfile", userId));

        if (req.getGrade() != null) profile.setGrade(req.getGrade());
        if (req.getSchoolName() != null) profile.setSchoolName(req.getSchoolName());
        if (req.getCity() != null) profile.setCity(req.getCity());
        studentProfileRepository.save(profile);
    }

    // ── Update teacher profile ────────────────────────────────────────────────

    @Transactional
    public void updateTeacherProfile(UUID userId, UpdateTeacherProfileRequest req) {
        TeacherProfile profile = teacherProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherProfile", userId));

        if (req.getBio() != null) profile.setBio(req.getBio());
        if (req.getHourlyRate() != null) profile.setHourlyRate(req.getHourlyRate());
        teacherProfileRepository.save(profile);
    }

    // ── Public teacher profile ────────────────────────────────────────────────

    public TeacherPublicProfileResponse getTeacherPublicProfile(UUID teacherId) {
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", teacherId));

        TeacherProfile profile = teacherProfileRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TeacherProfile", teacherId));

        List<Subject> subjects = teacherSubjectRepository.findByIdTeacherId(teacherId)
                .stream().map(ts -> ts.getId().getSubject()).collect(Collectors.toList());

        return TeacherPublicProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(profile.getBio())
                .hourlyRate(profile.getHourlyRate())
                .rating(profile.getRating())
                .isVerified(profile.getIsVerified())
                .subjects(subjects)
                .build();
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    @Transactional
    public void deleteMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setDeletedAt(Instant.now());
        user.setIsActive(false);
        userRepository.save(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractKeyFromUrl(String url) {
        // https://fra1.digitaloceanspaces.com/profile-image/avatars/uuid.png
        // key = avatars/uuid.png
        try {
            String[] parts = url.split("/profile-image/");
            return parts.length > 1 ? parts[1] : null;
        } catch (Exception e) {
            return null;
        }
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
