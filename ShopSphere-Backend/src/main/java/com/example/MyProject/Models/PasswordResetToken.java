package com.example.MyProject.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One password-reset request. The emailed link carries a random token; only its SHA-256 hash is
 * stored here, so someone who reads the database still cannot build a working reset link.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // SHA-256 of the token, as 64 hex characters.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Set when the link has been used (or replaced by a newer request). A used token never works again.
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}