package com.example.ecommerce_api.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DeviceResponse {

    private Long id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private Instant lastSeenAt;
    private Instant createdAt;

    /**
     * True if this row corresponds to the device the caller is currently
     * authenticated from.
     */
    private boolean current;

    /**
     * True if the device has at least one active (non-revoked, non-expired)
     * refresh token, i.e. it can still silently refresh its session.
     */
    private boolean active;
}
