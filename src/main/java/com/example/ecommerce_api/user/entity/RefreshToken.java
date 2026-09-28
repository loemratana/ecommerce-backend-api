package com.example.ecommerce_api.user.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;


@Entity
@Table(
        name = "refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_device_id", columnList = "device_id"),
                @Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_token_device")
    )
    private UserDevice device;

    /**
     * SHA-256 hash (hex) of the opaque refresh token. The raw token is never persisted.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * The token this row was rotated into. Kept for audit / reuse-detection tracing only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "replaced_by_id",
            foreignKey = @ForeignKey(name = "fk_refresh_token_replaced_by")
    )
    private RefreshToken replacedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
