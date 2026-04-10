package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.BadRequestException;
import com.example.academatebackend.common.exception.ConflictException;
import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.dto.CreateReviewRequest;
import com.example.academatebackend.dto.ReviewResponse;
import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.entity.Review;
import com.example.academatebackend.enums.LessonStatus;
import com.example.academatebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final LessonRepository lessonRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse create(UUID studentId, UUID teacherId, CreateReviewRequest req) {
        Lesson lesson = lessonRepository.findById(req.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", req.getLessonId()));

        if (!lesson.getStudentId().equals(studentId)) throw new BadRequestException("Bu dərs sizin deyil");
        if (!lesson.getTeacherId().equals(teacherId)) throw new BadRequestException("Müəllim ID uyğun deyil");
        if (lesson.getStatus() != LessonStatus.COMPLETED) throw new BadRequestException("Yalnız tamamlanmış dərslərə rəy yazıla bilər");
        if (reviewRepository.existsByLessonId(req.getLessonId())) throw new ConflictException("Bu dərs üçün artıq rəy yazılıb");

        Review review = Review.builder()
                .teacherId(teacherId)
                .studentId(studentId)
                .lessonId(req.getLessonId())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        reviewRepository.save(review);

        // Update teacher's average rating
        Double avg = reviewRepository.findAverageRatingByTeacherId(teacherId);
        if (avg != null) {
            teacherProfileRepository.findById(teacherId).ifPresent(p -> {
                p.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                teacherProfileRepository.save(p);
            });
        }

        return toResponse(review, studentId);
    }

    public Page<ReviewResponse> getTeacherReviews(UUID teacherId, Pageable pageable) {
        return reviewRepository.findByTeacherId(teacherId, pageable)
                .map(r -> toResponse(r, r.getStudentId()));
    }

    private ReviewResponse toResponse(Review r, UUID studentId) {
        var student = userRepository.findById(studentId).orElse(null);
        return ReviewResponse.builder()
                .id(r.getId())
                .studentId(r.getStudentId())
                .studentName(student != null ? student.getFullName() : null)
                .studentAvatarUrl(student != null ? student.getAvatarUrl() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
