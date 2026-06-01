package com.caicai.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String token) {
        String link = frontendUrl + "/api/auth/verify?token=" + token;
        sendEmail(
                toEmail,
                "Verify your Caicai account",
                "Welcome to Caicai!\n\nPlease verify your email by clicking the link below:\n\n"
                        + link + "\n\nThis link expires in 24 hours."
        );
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        sendEmail(
                toEmail,
                "Reset your Caicai password",
                "You requested a password reset.\n\nClick the link below to reset your password:\n\n"
                        + link + "\n\nThis link expires in 1 hour. If you didn't request this, ignore this email."
        );
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }
}