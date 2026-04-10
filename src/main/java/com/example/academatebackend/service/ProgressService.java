package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.ForbiddenException;
import com.example.academatebackend.dto.ProgressResponse;
import com.example.academatebackend.entity.StudentProgress;
import com.example.academatebackend.repository.ParentStudentLinkRepository;
import com.example.academatebackend.repository.StudentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final StudentProgressRepository progressRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;

    public List<ProgressResponse> getMyProgress(UUID studentId) {
        return progressRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProgressResponse> getChildProgress(UUID parentId, UUID studentId) {
        boolean linked = parentStudentLinkRepository
                .existsByIdParentIdAndIdStudentIdAndVerifiedTrue(parentId, studentId);
        if (!linked) {
            throw new ForbiddenException("You are not linked to this student");
        }
        return progressRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProgressResponse toResponse(StudentProgress p) {
        return ProgressResponse.builder()
                .id(p.getId())
                .studentId(p.getStudentId())
                .topicId(p.getTopicId())
                .masteryScore(p.getMasteryScore())
                .questionsAsked(p.getQuestionsAsked())
                .correctAnswers(p.getCorrectAnswers())
                .lastStudiedAt(p.getLastStudiedAt())
                .build();
    }
}
