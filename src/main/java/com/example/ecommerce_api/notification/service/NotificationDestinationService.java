package com.example.ecommerce_api.notification.service;

import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.notification.dto.NotificationDestinationRequest;
import com.example.ecommerce_api.notification.dto.NotificationDestinationResponse;

import java.util.List;
import java.util.Map;

/** Admin management of where notifications go (Settings → Notifications). */
public interface NotificationDestinationService {

    List<NotificationDestinationResponse> findAll();

    NotificationDestinationResponse findById(Long id);

    NotificationDestinationResponse create(NotificationDestinationRequest request);

    NotificationDestinationResponse update(Long id, NotificationDestinationRequest request);

    void delete(Long id);

    /** Sends a test message right away; throws BadRequestException with the provider's reason if it fails. */
    void sendTest(Long id);

    /** Whether each channel's credentials are set in the environment (never the credentials themselves). */
    Map<NotificationType, Boolean> channelStatus();
}
