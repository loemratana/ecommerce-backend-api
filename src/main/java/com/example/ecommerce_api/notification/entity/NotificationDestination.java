package com.example.ecommerce_api.notification.entity;

import com.example.ecommerce_api.Enum.NotificationEvent;
import com.example.ecommerce_api.Enum.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Where to send which events, managed by admins in Settings: e.g. the sales Telegram group gets
 * NEW_ORDER, the warehouse group gets LOW_STOCK. Credentials (bot token, SMTP) are not stored
 * here; they come from the environment.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "notification_destinations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_destinations_channel_target",
                columnNames = {"channel", "target"})
)
public class NotificationDestination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Admin's label, e.g. "Sales team". */
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType channel;

    /** Telegram chat id or @username, or an email address, depending on the channel. */
    @Column(nullable = false, length = 255)
    private String target;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_destination_events", joinColumns = @JoinColumn(name = "destination_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 30)
    private Set<NotificationEvent> events = new HashSet<>();

    /** Off = kept but not sent to, e.g. while a group is being set up. */
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
