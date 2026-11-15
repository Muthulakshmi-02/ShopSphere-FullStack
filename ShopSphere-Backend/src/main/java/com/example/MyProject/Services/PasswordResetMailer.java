package com.example.MyProject.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends the two password emails on a background thread (needs @EnableAsync, which you already have),
 * so the "forgot password" request answers at once instead of waiting for the mail server. It also
 * means the response time does not reveal whether an email address has an account.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetMailer {

    private final EmailService emailService;

    @Async
    public void sendResetLink(String to, String name, String link) {
        emailService.sendPasswordReset(to, name, link);
    }

    @Async
    public void sendPasswordChanged(String to, String name) {
        emailService.sendPasswordChanged(to, name);
    }
}