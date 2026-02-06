package org.papercloud.de.pdfsecurity.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    private final JavaMailSender emailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendVerificationEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Email Verification");
        message.setText("Please verify your email by clicking the link below:\n\n"
                + frontendUrl + "/verify-email?token=" + token);

        emailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("Please reset your password by clicking the link below:\n\n"
                + frontendUrl + "/reset-password?token=" + token);

        emailSender.send(message);
    }

}