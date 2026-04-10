package com.example.academatebackend.repository;

import com.example.academatebackend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByTeacherId(UUID teacherId, Pageable pageable);
    Optional<Review> findByLessonId(UUID lessonId);
    boolean existsByLessonId(UUID lessonId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.teacherId = :teacherId")
    Double findAverageRatingByTeacherId(UUID teacherId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.teacherId = :teacherId")
    Long countByTeacherId(UUID teacherId);
}
