package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.BadRequestException;
import com.example.academatebackend.common.exception.ConflictException;
import com.example.academatebackend.dto.InviteCodeResponse;
import com.example.academatebackend.dto.UserResponse;
import com.example.academatebackend.entity.ParentStudentLink;
import com.example.academatebackend.entity.StudentInviteCode;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.enums.LinkCreatedVia;
import com.example.academatebackend.enums.RelationType;
import com.example.academatebackend.repository.ParentStudentLinkRepository;
import com.example.academatebackend.repository.StudentInviteCodeRepository;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParentStudentLinkService {

    private final StudentInviteCodeRepository inviteCodeRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.security.student-invite-code.expiration:PT24H}")
    private Duration codeExpiration;

    // ── Student: generate invite code ─────────────────────────────────────────

    @Transactional
    public InviteCodeResponse generateInviteCode(UUID studentId) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        StudentInviteCode inviteCode = StudentInviteCode.builder()
                .studentId(studentId)
                .code(code)
                .expiresAt(Instant.now().plus(codeExpiration))
                .build();
        inviteCodeRepository.save(inviteCode);

        return InviteCodeResponse.builder()
                .code(code)
                .expiresAt(inviteCode.getExpiresAt())
                .build();
    }

    // ── Parent: link to student via invite code ────────────────────────────────

    @Transactional
    public void linkByInviteCode(UUID parentId, String code, RelationType relation) {
        StudentInviteCode inviteCode = inviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Invalid invite code"));

        if (inviteCode.isExpired()) {
            throw new BadRequestException("Invite code has expired");
        }
        if (inviteCode.isUsed()) {
            throw new BadRequestException("Invite code has already been used");
        }

        UUID studentId = inviteCode.getStudentId();

        boolean alreadyLinked = parentStudentLinkRepository
                .existsByIdParentIdAndIdStudentIdAndVerifiedTrue(parentId, studentId);
        if (alreadyLinked) {
            throw new ConflictException("Already linked to this student");
        }

        inviteCode.setUsedAt(Instant.now());
        inviteCode.setUsedBy(parentId);
        inviteCodeRepository.save(inviteCode);

        ParentStudentLink link = ParentStudentLink.builder()
                .id(new ParentStudentLink.ParentStudentLinkId(parentId, studentId))
                .relation(relation)
                .verified(true)
                .createdVia(LinkCreatedVia.INVITE_CODE)
                .linkedAt(Instant.now())
                .build();
        parentStudentLinkRepository.save(link);

        User student = userRepository.findById(studentId).orElse(null);
        User parent = userRepository.findById(parentId).orElse(null);
        if (student != null && parent != null) {
            emailService.sendParentLinkNotification(student, parent);
        }
    }

    // ── Parent: list linked children ──────────────────────────────────────────

    public List<UserResponse> getChildren(UUID parentId) {
        return parentStudentLinkRepository.findByIdParentId(parentId).stream()
                .filter(link -> Boolean.TRUE.equals(link.getVerified()))
                .map(link -> userRepository.findById(link.getId().getStudentId()).orElse(null))
                .filter(u -> u != null)
                .map(this::toUserResponse)
                .toList();
    }

    // ── Student: list linked parents ──────────────────────────────────────────

    public List<UserResponse> getParents(UUID studentId) {
        return parentStudentLinkRepository.findByIdStudentId(studentId).stream()
                .filter(link -> Boolean.TRUE.equals(link.getVerified()))
                .map(link -> userRepository.findById(link.getId().getParentId()).orElse(null))
                .filter(u -> u != null)
                .map(this::toUserResponse)
                .toList();
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
