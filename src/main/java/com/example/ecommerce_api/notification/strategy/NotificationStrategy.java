package com.example.ecommerce_api.notification.strategy;

import com.example.ecommerce_api.Enum.NotificationType;

/**
 * Strategy for one delivery channel. Implementations only know how to deliver a message to one
 * target on their channel; which destinations get which event is admin data
 * (notification_destinations) and the dispatching lives in NotificationService, so every channel
 * behaves the same.
 *
 * Add a channel by writing a @Component that implements this; NotificationFactory picks it up.
 */
public interface NotificationStrategy {

    NotificationType type();

    /** False while the channel's credentials are missing, so every send would fail. */
    boolean isConfigured();

    /**
     * Delivers the message to one target: a Telegram chat id or an email address.
     *
     * @throws NotificationException when the channel isn't configured or the provider refuses
     */
    void send(String target, NotificationMessage message);
}
