package com.example.ecommerce_api.notification.service;

import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Target validation for admin-entered destinations. */
class NotificationDestinationServiceImplTest {

    @Test
    void acceptsTelegramChatIdsAndChannelUsernames() {
        assertThat(NotificationDestinationServiceImpl.normalizeTarget(NotificationType.TELEGRAM, " -1001234567890 "))
                .isEqualTo("-1001234567890");
        assertThat(NotificationDestinationServiceImpl.normalizeTarget(NotificationType.TELEGRAM, "@shop_alerts"))
                .isEqualTo("@shop_alerts");
    }

    @Test
    void rejectsAnythingElseForTelegram() {
        assertThatThrownBy(() -> NotificationDestinationServiceImpl.normalizeTarget(NotificationType.TELEGRAM, "https://t.me/+abc"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> NotificationDestinationServiceImpl.normalizeTarget(NotificationType.TELEGRAM, ""))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void emailTargetsAreValidatedAndLowerCased() {
        assertThat(NotificationDestinationServiceImpl.normalizeTarget(NotificationType.EMAIL, " Buyer@Example.com"))
                .isEqualTo("buyer@example.com");
        assertThatThrownBy(() -> NotificationDestinationServiceImpl.normalizeTarget(NotificationType.EMAIL, "not-an-email"))
                .isInstanceOf(BadRequestException.class);
    }
}
