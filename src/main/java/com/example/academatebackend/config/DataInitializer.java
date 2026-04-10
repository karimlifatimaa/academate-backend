package com.example.academatebackend.config;

import com.example.academatebackend.entity.User;
import com.example.academatebackend.enums.Role;
import com.example.academatebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "kerimlifatime417@gmail.com";

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            User admin = User.builder()
                    .email(ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode("Fatima@2024!"))
                    .fullName("Fatima")
                    .role(Role.ADMIN)
                    .emailVerifiedAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.info("Admin hesabi yaradildi: {}", ADMIN_EMAIL);
        }
    }
}
