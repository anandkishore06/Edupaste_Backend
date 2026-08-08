package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "verification_channel", nullable = false, length = 20)
    private String verificationChannel; // "EMAIL" or "MOBILE"

    @Column(name = "recipient_masked", nullable = false, length = 150)
    private String recipientMasked;

    @Column(name = "hashed_code", nullable = false, length = 255)
    private String hashedCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @Builder.Default
    @Column(name = "max_attempts")
    private Integer maxAttempts = 5;

    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reset_token", length = 255)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Builder.Default
    @Column(name = "is_used")
    private Boolean isUsed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
