package com.example.ecommerce_api.notification.dto;

import com.example.ecommerce_api.Enum.NotificationEvent;
import com.example.ecommerce_api.Enum.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDestinationRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Channel is required")
    private NotificationType channel;

    /** TELEGRAM: chat id such as -1001234567890, or @channelusername. EMAIL: an email address. */
    @NotBlank(message = "Target is required")
    @Size(max = 255, message = "Target must be at most 255 characters")
    private String target;

    @NotEmpty(message = "Choose at least one event")
    private Set<NotificationEvent> events;

    /** Defaults to true. */
    private Boolean enabled;
}
