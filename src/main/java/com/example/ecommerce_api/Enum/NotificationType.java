package com.example.ecommerce_api.Enum;

/** A channel notifications are delivered on. Each value has one NotificationStrategy. */
public enum NotificationType {
    /** A Telegram group, supergroup or channel; the target is its chat id (e.g. -1001234567890) or @username. */
    TELEGRAM,
    /** An email address; sent over the SMTP server in spring.mail.*. */
    EMAIL
}
