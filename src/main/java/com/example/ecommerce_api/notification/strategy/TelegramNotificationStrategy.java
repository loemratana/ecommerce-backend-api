package com.example.ecommerce_api.notification.strategy;

import com.example.ecommerce_api.Enum.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Posts to a Telegram group through the Bot API (sendMessage). One bot (TELEGRAM_BOT_TOKEN) serves
 * every group; admins add the bot to a group and register the group's chat id as a destination.
 *
 * The token is part of every request URL, and RestClient puts the URL in its exception messages,
 * so those messages are never logged or passed on.
 */
@Component
public class TelegramNotificationStrategy implements NotificationStrategy {

    // Telegram rejects longer messages
    static final int MAX_TEXT_LENGTH = 4096;

    private final String botToken;
    private final RestClient restClient;

    @Autowired
    public TelegramNotificationStrategy(
            @Value("${app.notifications.telegram.bot-token:}") String botToken,
            @Value("${app.notifications.telegram.api-url:https://api.telegram.org}") String apiUrl) {
        this(botToken, RestClient.builder().baseUrl(apiUrl).requestFactory(requestFactory()));
    }

    /** Tests pass a builder bound to MockRestServiceServer. */
    TelegramNotificationStrategy(String botToken, RestClient.Builder restClientBuilder) {
        this.botToken = botToken == null ? "" : botToken.trim();
        this.restClient = restClientBuilder.build();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return requestFactory;
    }

    @Override
    public NotificationType type() {
        return NotificationType.TELEGRAM;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(botToken);
    }

    @Override
    public void send(String target, NotificationMessage message) {
        if (!isConfigured()) {
            throw new NotificationException("Telegram is not configured: set TELEGRAM_BOT_TOKEN");
        }
        try {
            restClient.post()
                    // Not a {token} variable: variables are fully encoded, turning the token's ':' into %3A
                    .uri("/bot" + botToken + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", target,
                            "text", text(message),
                            "disable_web_page_preview", true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // e.g. 400 "Bad Request: chat not found", 403 "Forbidden: bot was kicked from the group chat"
            throw new NotificationException("Telegram refused the message (HTTP " + e.getStatusCode().value()
                    + "): " + describe(e));
        } catch (RestClientException e) {
            throw new NotificationException("Telegram could not be reached (" + e.getClass().getSimpleName() + ")");
        }
    }

    static String text(NotificationMessage message) {
        String text = message.title() + "\n\n" + message.body();
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH - 1) + "…";
    }

    /** Telegram's own reason, from its {"ok":false,"description":"..."} error body. */
    private static String describe(RestClientResponseException e) {
        try {
            TelegramError error = e.getResponseBodyAs(TelegramError.class);
            if (error != null && StringUtils.hasText(error.description())) {
                return error.description();
            }
        } catch (RuntimeException ignored) {
            // not JSON
        }
        return "no details";
    }

    private record TelegramError(Boolean ok, String description) {
    }
}
