package com.example.ecommerce_api.notification.strategy;

/**
 * A message could not be delivered. The message is safe to log and to show an admin: strategies
 * never put credentials in it (a Telegram request URL contains the bot token), and never attach
 * the provider's exception as cause for the same reason.
 */
public class NotificationException extends RuntimeException {
    public NotificationException(String message) {
        super(message);
    }
}
