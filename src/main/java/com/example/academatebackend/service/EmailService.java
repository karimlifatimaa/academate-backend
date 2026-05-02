package com.example.academatebackend.service;

import com.example.academatebackend.entity.Lesson;
import com.example.academatebackend.entity.User;
import com.example.academatebackend.util.IcsGenerator;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
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

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@academate.az}")
    private String fromAddress;

    @Async
    public void sendVerificationEmail(User user, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
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
        String link = frontendUrl + "/reset-password?token=" + token;
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
    public void sendNewTeacherForVerification(String adminEmail, User teacher) {
        String reviewLink = frontendUrl + "/admin/teachers";
        String html = """
                <h2>Academate — Yeni müəllim qeydiyyatı</h2>
                <p>Yeni bir müəllim hesab yaratdı və təsdiqlənməyi gözləyir:</p>
                <ul>
                  <li><strong>Ad:</strong> %s</li>
                  <li><strong>Email:</strong> %s</li>
                </ul>
                <p>Profili yoxlamaq və təsdiqləmək üçün:</p>
                <a href="%s" style="
                    display:inline-block;padding:12px 24px;
                    background:#4A6741;color:#fff;
                    border-radius:6px;text-decoration:none;font-weight:bold;">
                  Müəllimləri yoxla
                </a>
                """.formatted(
                        teacher.getFullName(),
                        teacher.getEmail() != null ? teacher.getEmail() : "—",
                        reviewLink);
        send(adminEmail, "Academate — Yeni müəllim təsdiq gözləyir", html);
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

    @Async
    public void sendLessonConfirmationEmail(User recipient, User teacher, Lesson lesson, String meetLink) {
        String dateStr = lesson.getScheduledAt()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        String linkSection = meetLink != null
                ? """
                  <a href="%s" style="
                      display:inline-block;padding:12px 24px;
                      background:#00C4B4;color:#fff;
                      border-radius:6px;text-decoration:none;font-weight:bold;">
                    Zoom-a Qoşul
                  </a>
                  """.formatted(meetLink)
                : "<p>Görüşmə linki tezliklə əlavə ediləcək.</p>";

        String html = """
                <h2 style="color:#3B3F8C;">Academate — Dərs Təsdiqləndi ✓</h2>
                <p>Salam, <strong>%s</strong>!</p>
                <p>Aşağıdakı dərsiniz təsdiqləndi:</p>
                <table style="border-collapse:collapse;width:100%%;max-width:400px;">
                  <tr><td style="padding:8px;color:#6B7280;">Müəllim</td><td style="padding:8px;font-weight:bold;">%s</td></tr>
                  <tr><td style="padding:8px;color:#6B7280;">Fənn</td><td style="padding:8px;font-weight:bold;">%s</td></tr>
                  <tr><td style="padding:8px;color:#6B7280;">Tarix/Saat</td><td style="padding:8px;font-weight:bold;">%s</td></tr>
                  <tr><td style="padding:8px;color:#6B7280;">Müddət</td><td style="padding:8px;font-weight:bold;">%d dəqiqə</td></tr>
                </table>
                <br/>
                %s
                <p style="color:#6B7280;font-size:13px;margin-top:16px;">
                  Dərsə qoşulmaq üçün yuxarıdakı düyməyə klikləyin. Uğurlar!
                </p>
                """.formatted(
                recipient.getFullName(),
                teacher.getFullName(),
                lesson.getSubject(),
                dateStr,
                lesson.getDurationMinutes(),
                linkSection);

        sendWithIcs(recipient.getEmail(), "Academate — Dərs Təsdiqləndi: " + dateStr, html,
                IcsGenerator.generate(lesson, teacher, meetLink));
    }

    @Async
    public void sendLessonReminderEmail(User recipient, User teacher, Lesson lesson) {
        String dateStr = lesson.getScheduledAt()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        String linkSection = lesson.getMeetingLink() != null
                ? """
                  <a href="%s" style="
                      display:inline-block;padding:12px 24px;
                      background:#3B3F8C;color:#fff;
                      border-radius:6px;text-decoration:none;font-weight:bold;">
                    Dərsə Qoşul
                  </a>
                  """.formatted(lesson.getMeetingLink())
                : "";

        String html = """
                <h2 style="color:#3B3F8C;">Academate — Dərsinizə 1 Saat Qaldı ⏰</h2>
                <p>Salam, <strong>%s</strong>!</p>
                <p>Dərsiniz <strong>1 saat</strong> sonra başlayır:</p>
                <table style="border-collapse:collapse;width:100%%;max-width:400px;">
                  <tr><td style="padding:8px;color:#6B7280;">Müəllim</td><td style="padding:8px;font-weight:bold;">%s</td></tr>
                  <tr><td style="padding:8px;color:#6B7280;">Fənn</td><td style="padding:8px;font-weight:bold;">%s</td></tr>
                  <tr><td style="padding:8px;color:#6B7280;">Saat</td><td style="padding:8px;font-weight:bold;">%s</td></tr>
                </table>
                <br/>
                %s
                """.formatted(
                recipient.getFullName(),
                teacher.getFullName(),
                lesson.getSubject(),
                dateStr,
                linkSection);

        sendWithIcs(recipient.getEmail(), "Academate — Dərsinizə 1 saat qaldı!", html,
                IcsGenerator.generate(lesson, teacher, lesson.getMeetingLink()));
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

    private void sendWithIcs(String to, String subject, String htmlBody, String icsContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment("ders.ics",
                    new ByteArrayDataSource(icsContent.getBytes(), "text/calendar;charset=UTF-8"));
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
