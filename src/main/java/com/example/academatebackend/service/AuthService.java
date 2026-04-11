package com.example.academatebackend.service;

import com.example.academatebackend.common.exception.BadRequestException;
import com.example.academatebackend.common.exception.ConflictException;
import com.example.academatebackend.common.exception.ResourceNotFoundException;
import com.example.academatebackend.common.exception.UnauthorizedException;
import com.example.academatebackend.dto.*;
import com.example.academatebackend.entity.*;
import com.example.academatebackend.enums.LinkCreatedVia;
import com.example.academatebackend.enums.Role;
import com.example.academatebackend.repository.*;
import com.example.academatebackend.security.CustomUserDetails;
import com.example.academatebackend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.access-token-expiration}")
    private Duration accessTokenExpiration;

    @Value("${app.security.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.login.lock-duration:PT15M}")
    private Duration lockDuration;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerStudent(RegisterStudentRequest req, HttpServletRequest request) {
        validateAgeMinimum(req.getBirthDate(), 5);

        boolean isChild = isChildAge(req.getBirthDate());
        if (!isChild && (req.getEmail() == null || req.getPassword() == null)) {
            throw new BadRequestException("Email and password are required for students aged 7+");
        }
        if (!isChild) {
            checkEmailUniqueness(req.getEmail());
        }

        User user = User.builder()
                .email(isChild ? null : req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword() != null ? req.getPassword() : UUID.randomUUID().toString()))
                .fullName(req.getFullName())
                .role(Role.STUDENT)
                .build();
        userRepository.save(user);

        StudentProfile profile = StudentProfile.builder()
                .userId(user.getId())
                .user(user)
                .grade(req.getGrade())
                .schoolName(req.getSchoolName())
                .city(req.getCity())
                .birthDate(req.getBirthDate())
                .build();
        studentProfileRepository.save(profile);

        if (!isChild) {
            emailVerificationService.sendVerificationEmail(user);
        }

        return buildAuthResponse(user, request);
    }

    @Transactional
    public UserResponse registerChild(RegisterChildRequest req, UUID parentUserId, HttpServletRequest request) {
        validateAgeMinimum(req.getBirthDate(), 5);

        String username = generateUsername();
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User childUser = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .fullName(req.getFullName())
                .role(Role.STUDENT)
                .build();
        userRepository.save(childUser);

        StudentProfile profile = StudentProfile.builder()
                .userId(childUser.getId())
                .user(childUser)
                .grade(req.getGrade())
                .schoolName(req.getSchoolName())
                .city(req.getCity())
                .birthDate(req.getBirthDate())
                .build();
        studentProfileRepository.save(profile);

        ParentStudentLink link = ParentStudentLink.builder()
                .id(new ParentStudentLink.ParentStudentLinkId(parentUserId, childUser.getId()))
                .relation(com.example.academatebackend.enums.RelationType.GUARDIAN)
                .verified(true)
                .createdVia(LinkCreatedVia.PARENT_CREATED)
                .linkedAt(Instant.now())
                .build();
        parentStudentLinkRepository.save(link);

        return toUserResponse(childUser);
    }

    @Transactional
    public AuthResponse registerTeacher(RegisterTeacherRequest req, HttpServletRequest request) {
        checkEmailUniqueness(req.getEmail());

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role(Role.TEACHER)
                .phone(req.getPhone())
                .build();
        userRepository.save(user);

        TeacherProfile profile = TeacherProfile.builder()
                .userId(user.getId())
                .user(user)
                .build();
        teacherProfileRepository.save(profile);

        if (req.getSubjects() != null) {
            req.getSubjects().forEach(subject -> {
                TeacherSubject ts = TeacherSubject.builder()
                        .id(new TeacherSubject.TeacherSubjectId(user.getId(), subject))
                        .build();
                teacherSubjectRepository.save(ts);
            });
        }

        emailVerificationService.sendVerificationEmail(user);
        return buildAuthResponse(user, request);
    }

    @Transactional
    public AuthResponse registerParent(RegisterParentRequest req, HttpServletRequest request) {
        checkEmailUniqueness(req.getEmail());

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role(Role.PARENT)
                .phone(req.getPhone())
                .build();
        userRepository.save(user);

        ParentProfile profile = ParentProfile.builder()
                .userId(user.getId())
                .user(user)
                .build();
        parentProfileRepository.save(profile);

        emailVerificationService.sendVerificationEmail(user);
        return buildAuthResponse(user, request);
    }

    // ── Login / Logout ────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest request) {
        User user = userRepository.findByEmailOrUsername(req.getIdentifier(), req.getIdentifier())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.isLocked()) {
            throw new UnauthorizedException("Account is locked until " + user.getLockedUntil());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getIdentifier(), req.getPassword())
            );
        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setFailedLoginAttempts((short) 0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user, request);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest req, HttpServletRequest request) {
        var token = refreshTokenService.validate(req.getRefreshToken());
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newRefreshToken = refreshTokenService.rotate(req.getRefreshToken(), request);
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(accessTokenExpiration.getSeconds())
                .user(toUserResponse(user))
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest req) {
        refreshTokenService.revoke(req.getRefreshToken());
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenService.revokeAll(userId);
    }

    // ── Password ──────────────────────────────────────────────────────────────

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ── Me ───────────────────────────────────────────────────────────────────

    @Transactional
    public void resendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        emailVerificationService.sendVerificationEmail(user);
    }

    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toUserResponse(user);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void handleFailedLogin(User user) {
        short attempts = (short) (user.getFailedLoginAttempts() + 1);
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(Instant.now().plus(lockDuration));
        }
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, HttpServletRequest request) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.generate(user.getId(), request);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiration.getSeconds())
                .user(toUserResponse(user))
                .build();
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

    private void checkEmailUniqueness(String email) {
        if (email != null && userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use: " + email);
        }
    }

    private void validateAgeMinimum(java.time.LocalDate birthDate, int minAge) {
        if (birthDate == null) return;
        int age = java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
        if (age < minAge) {
            throw new BadRequestException("Minimum age is " + minAge);
        }
    }

    private boolean isChildAge(java.time.LocalDate birthDate) {
        if (birthDate == null) return false;
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears() < 7;
    }

    private String generateUsername() {
        return "student_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
