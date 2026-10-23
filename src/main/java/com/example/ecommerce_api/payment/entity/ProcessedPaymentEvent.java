package com.example.ecommerce_api.payment.entity;

import com.example.ecommerce_api.Enum.PaymentMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** A gateway event that has been handled; the same status can be reported more than once, so a repeat is ignored. */
@Getter
@Entity
@Table(
        name = "processed_payment_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_processed_payment_event", columnNames = {"method", "event_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedPaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public ProcessedPaymentEvent(PaymentMethod method, String eventId) {
        this.method = method;
        this.eventId = eventId;
    }
}
