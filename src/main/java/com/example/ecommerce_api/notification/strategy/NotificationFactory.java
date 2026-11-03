package com.example.ecommerce_api.notification.strategy;

import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Picks the strategy for a channel; Spring injects every NotificationStrategy bean. */
@Component
public class NotificationFactory {

    private final Map<NotificationType, NotificationStrategy> strategies;

    public NotificationFactory(List<NotificationStrategy> strategies) {
        Map<NotificationType, NotificationStrategy> byType = new EnumMap<>(NotificationType.class);
        for (NotificationStrategy strategy : strategies) {
            NotificationStrategy previous = byType.put(strategy.type(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Two notification strategies for " + strategy.type() + ": "
                        + previous.getClass().getSimpleName() + " and " + strategy.getClass().getSimpleName());
            }
        }
        this.strategies = Collections.unmodifiableMap(byType);
    }

    public NotificationStrategy get(NotificationType type) {
        NotificationStrategy strategy = type == null ? null : strategies.get(type);
        if (strategy == null) {
            throw new BadRequestException("Notification channel " + type + " is not available");
        }
        return strategy;
    }

    /** Whether each channel has its credentials, for the admin settings page. */
    public Map<NotificationType, Boolean> configuredChannels() {
        Map<NotificationType, Boolean> configured = new EnumMap<>(NotificationType.class);
        strategies.forEach((type, strategy) -> configured.put(type, strategy.isConfigured()));
        return configured;
    }
}
