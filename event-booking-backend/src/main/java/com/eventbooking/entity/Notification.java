package com.eventbooking.entity;

import com.eventbooking.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
