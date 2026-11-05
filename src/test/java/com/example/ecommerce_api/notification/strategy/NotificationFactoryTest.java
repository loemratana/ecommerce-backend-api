package com.example.ecommerce_api.notification.strategy;

import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Strategy selection, without Spring. */
class NotificationFactoryTest {

    /** Records sends instead of calling a provider. */
    static class FakeStrategy implements NotificationStrategy {
        final NotificationType type;
        final boolean configured;

        FakeStrategy(NotificationType type, boolean configured) {
            this.type = type;
            this.configured = configured;
        }

        @Override public NotificationType type() { return type; }
        @Override public boolean isConfigured() { return configured; }
        @Override public void send(String target, NotificationMessage message) { }
    }

    private final FakeStrategy telegram = new FakeStrategy(NotificationType.TELEGRAM, true);
    private final FakeStrategy email = new FakeStrategy(NotificationType.EMAIL, false);

    @Test
    void picksTheStrategyForEachType() {
        NotificationFactory factory = new NotificationFactory(List.of(telegram, email));

        assertThat(factory.get(NotificationType.TELEGRAM)).isSameAs(telegram);
        assertThat(factory.get(NotificationType.EMAIL)).isSameAs(email);
    }

    @Test
    void typeWithoutAStrategyIsRejected() {
        NotificationFactory factory = new NotificationFactory(List.of(telegram));

        assertThatThrownBy(() -> factory.get(NotificationType.EMAIL))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("EMAIL is not available");
        assertThatThrownBy(() -> factory.get(null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void twoStrategiesForOneTypeFailAtStartup() {
        FakeStrategy otherTelegram = new FakeStrategy(NotificationType.TELEGRAM, true);

        assertThatThrownBy(() -> new NotificationFactory(List.of(telegram, otherTelegram)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Two notification strategies for TELEGRAM");
    }

    @Test
    void reportsWhichChannelsAreConfigured() {
        NotificationFactory factory = new NotificationFactory(List.of(telegram, email));

        assertThat(factory.configuredChannels())
                .containsEntry(NotificationType.TELEGRAM, true)
                .containsEntry(NotificationType.EMAIL, false);
    }
}
