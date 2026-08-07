package com.edupaste.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "notification_type", length = 50)
    @Builder.Default
    private String notificationType = "EMAIL";

    @Column(name = "channel", length = 50)
    @Builder.Default
    private String channel = "EMAIL";

    @Column(name = "event", length = 100)
    @Builder.Default
    private String event = "ADMISSION_SUBMITTED";

    @Column(name = "application_number", length = 50)
    private String applicationNumber;

    @Column(nullable = false, length = 20)
    private String status; // SENT, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
