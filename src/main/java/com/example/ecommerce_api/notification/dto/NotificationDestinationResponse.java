package com.example.ecommerce_api.notification.dto;

import com.example.ecommerce_api.Enum.NotificationEvent;
import com.example.ecommerce_api.Enum.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Getter
public class NotificationDestinationResponse {

    private Long id;
    private String name;
    private NotificationType channel;
    private String target;
    private Set<NotificationEvent> events;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
