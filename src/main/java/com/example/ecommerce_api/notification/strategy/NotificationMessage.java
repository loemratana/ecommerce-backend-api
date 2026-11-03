package com.example.ecommerce_api.notification.strategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * What to send, independent of the channel.
 *
 * @param title         email subject; first line of the Telegram message
 * @param body          plain text: the Telegram message and the email's text alternative
 * @param emailTemplate Thymeleaf template for the HTML email, e.g. "email/new-order"; null = the generic
 *                      "email/notification" template, which shows title and body
 * @param model         variables for the template (plain DTOs and values, no JPA entities: the email is
 *                      rendered on a background thread after the transaction has ended)
 */
public record NotificationMessage(String title, String body, String emailTemplate, Map<String, Object> model) {

    public NotificationMessage {
        // HashMap copy: template variables may be null (e.g. a product without a brand)
        model = model == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(model));
    }

    /** A message without its own email template. */
    public NotificationMessage(String title, String body) {
        this(title, body, null, Map.of());
    }
}
