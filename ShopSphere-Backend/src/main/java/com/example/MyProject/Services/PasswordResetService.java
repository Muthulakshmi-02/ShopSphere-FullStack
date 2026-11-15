package com.example.MyProject.Services;

import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.TooManyRequestsException;
import com.example.MyProject.Models.PasswordResetToken;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.PasswordResetTokenRepository;
import com.example.MyProject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.MyProject.Models.AttemptLimiter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    /** A reset link works for this long. */
    private static final int TOKEN_MINUTES = 30;

    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_EMAIL = 3;     // per hour
    private static final int MAX_REQUESTS_PER_IP = 10;       // per hour
    private static final int MAX_RESET_ATTEMPTS_PER_IP = 20; // per hour

    private static final String INVALID_LINK =
            "This reset link is invalid or has expired. Please request a new one.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttemptLimiter limiter;
    private final PasswordResetMailer mailer;

    // Where the Angular app lives; the emailed link points there.
    @Value("${app.frontend.base-url:http://localhost:4500}")
    private String frontendBaseUrl;

    private final SecureRandom random = new SecureRandom();

    /**
     * Step 1: the customer asked for a reset link.
     * The caller always gets the same answer whether or not the email has an account, so this cannot
     * be used to find out which emails are registered.
     */
    @Transactional
    public void requestReset(String rawEmail, String clientIp) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String ip = clientIp == null ? "unknown" : clientIp;
        String ipKey = "forgot:ip:" + ip;
        String emailKey = "forgot:email:" + email;

        // Too many requests from one place: say so openly.
        if (limiter.isLimited(ipKey, MAX_REQUESTS_PER_IP)) {
            throw new TooManyRequestsException("Too many requests. Please try again later.");
        }
        limiter.hit(ipKey, REQUEST_WINDOW);

        // Too many for one email: answer normally but send nothing, so a stranger cannot flood
        // someone's inbox (and still learns nothing about the account).
        boolean emailLimited = limiter.isLimited(emailKey, MAX_REQUESTS_PER_EMAIL);
        limiter.hit(emailKey, REQUEST_WINDOW);
        if (emailLimited) {
            return;
        }

        userRepository.findFirstByEmailIgnoreCase(email).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();

            // Only the newest link may work.
            tokenRepository.invalidateAllForUser(user.getUserId(), now);

            String rawToken = newRawToken();
            tokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(sha256Hex(rawToken))
                    .expiresAt(now.plusMinutes(TOKEN_MINUTES))
                    .build());

            String link = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;
            mailer.sendResetLink(user.getEmail(), user.getUserName(), link);
        });
    }

    /** Step 2: the customer opened the link and chose a new password. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword, String clientIp) {
        String ipKey = "reset:ip:" + (clientIp == null ? "unknown" : clientIp);
        if (limiter.isLimited(ipKey, MAX_RESET_ATTEMPTS_PER_IP)) {
            throw new TooManyRequestsException("Too many attempts. Please try again later.");
        }
        limiter.hit(ipKey, REQUEST_WINDOW);

        PasswordResetToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken.trim()))
                .orElseThrow(() -> new CartBusinessException(INVALID_LINK));

        LocalDateTime now = LocalDateTime.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new CartBusinessException(INVALID_LINK);
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // This link, and any other open one, can never be used again.
        tokenRepository.invalidateAllForUser(user.getUserId(), now);

        // The customer can sign in straight away, even if wrong guesses had locked them out.
        limiter.reset("login:email:" + user.getEmail().trim().toLowerCase(Locale.ROOT));

        // Tell them, so a stranger changing the password cannot go unnoticed.
        mailer.sendPasswordChanged(user.getEmail(), user.getUserName());
    }

    /** Old rows are of no use to anyone. */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void deleteOldTokens() {
        int removed = tokenRepository.deleteExpiredBefore(LocalDateTime.now().minusDays(1));
        if (removed > 0) {
            log.info("Deleted {} old password reset token(s)", removed);
        }
    }

    // --- helpers ---

    // 32 random bytes = 256 bits: impossible to guess. URL-safe, so it works inside a link.
    private String newRawToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}