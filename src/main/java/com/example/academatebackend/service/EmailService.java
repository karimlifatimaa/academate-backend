package com.example.academatebackend.service;

import com.example.academatebackend.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@academate.az}")
    private String fromAddress;

    @Async
    public void sendVerificationEmail(User user, String token) {
        String link = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        String html = """
                <h2>Academate — Email Təsdiqi</h2>
                <p>Salam, <strong>%s</strong>!</p>
                <p>Emailinizi təsdiq etmək üçün aşağıdakı düyməyə klikləyin:</p>
                <a href="%s" style="
                    display:inline-block;padding:12px 24px;
                    background:#4F46E5;color:#fff;
                    border-radius:6px;text-decoration:none;font-weight:bold;">
                  Emaili Təsdiqlə
                </a>
                <p style="color:#6B7280;font-size:13px;margin-top:16px;">
                  Link 24 saat ərzində etibarlıdır.
                </p>
                """.formatted(user.getFullName(), link);
        send(user.getEmail(), "Academate — Email Təsdiqi", html);
    }

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String link = baseUrl + "/api/v1/auth/reset-password?token=" + token;
        String html = """
                <h2>Academate — Şifrə Bərpası</h2>
                <p>Salam, <strong>%s</strong>!</p>
                <p>Şifrənizi sıfırlamaq üçün aşağıdakı düyməyə klikləyin:</p>
                <a href="%s" style="
                    display:inline-block;padding:12px 24px;
                    background:#DC2626;color:#fff;
                    border-radius:6px;text-decoration:none;font-weight:bold;">
                  Şifrəni Sıfırla
                </a>
                <p style="color:#6B7280;font-size:13px;margin-top:16px;">
                  Link 15 dəqiqə ərzində etibarlıdır.
                  Bu sorğunu siz göndərməmisinizsə, məktubu nəzərə almayın.
                </p>
                """.formatted(user.getFullName(), link);
        send(user.getEmail(), "Academate — Şifrə Bərpası", html);
    }

    @Async
    public void sendParentLinkNotification(User student, User parent) {
        if (student.getEmail() == null) return;
        String html = """
                <h2>Academate — Valideyn Bağlantısı</h2>
                <p>Salam, <strong>%s</strong>!</p>
                <p><strong>%s</strong> sizin valideyn/qəyyumunuz kimi əlaqə qurmaq istəyir.</p>
                <p>Təsdiqləmək üçün profilinizə daxil olun.</p>
                """.formatted(student.getFullName(), parent.getFullName());
        send(student.getEmail(), "Academate — Valideyn Bağlantısı", html);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
