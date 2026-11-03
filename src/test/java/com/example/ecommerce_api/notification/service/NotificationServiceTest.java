package com.example.ecommerce_api.notification.service;

import com.example.ecommerce_api.Enum.NotificationEvent;
import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.notification.entity.NotificationDestination;
import com.example.ecommerce_api.notification.repository.NotificationDestinationRepository;
import com.example.ecommerce_api.notification.strategy.NotificationException;
import com.example.ecommerce_api.notification.strategy.NotificationFactory;
import com.example.ecommerce_api.notification.strategy.NotificationMessage;
import com.example.ecommerce_api.notification.strategy.NotificationStrategy;
import com.example.ecommerce_api.order.dto.OrderCustomerResponse;
import com.example.ecommerce_api.order.dto.OrderResponse;
import com.example.ecommerce_api.settings.dto.SettingsResponse;
import com.example.ecommerce_api.settings.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Dispatching: who gets what, when, and that provider failures stay contained. Runs on the calling thread. */
class NotificationServiceTest {

    /** Records what it was asked to send; can be told to fail for one target. */
    static class RecordingStrategy implements NotificationStrategy {
        final NotificationType type;
        final List<String> sentTo = new ArrayList<>();
        final List<String> titles = new ArrayList<>();
        boolean configured = true;
        String failFor;
        RuntimeException failure;

        RecordingStrategy(NotificationType type) {
            this.type = type;
        }

        @Override public NotificationType type() { return type; }
        @Override public boolean isConfigured() { return configured; }

        @Override
        public void send(String target, NotificationMessage message) {
            if (target.equals(failFor)) throw failure;
            sentTo.add(target);
            titles.add(message.title());
        }
    }

    private final NotificationDestinationRepository repository = mock(NotificationDestinationRepository.class);
    private final NotificationMessages messages = mock(NotificationMessages.class);
    private final RecordingStrategy telegram = new RecordingStrategy(NotificationType.TELEGRAM);
    private final RecordingStrategy email = new RecordingStrategy(NotificationType.EMAIL);
    private final NotificationFactory factory = new NotificationFactory(List.of(telegram, email));

    private final NotificationMessage message = new NotificationMessage("⚠️ Low stock: Tee", "SKU: TEE-M");

    /** Settings with only the notification switch filled in. */
    private static SettingsService settings(boolean customerOrderEmail) {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.get()).thenReturn(new SettingsResponse(null, null, null,
                new SettingsResponse.Notifications(customerOrderEmail), null, null, null, null));
        return settingsService;
    }

    private NotificationService service(boolean enabled) {
        return new NotificationService(repository, factory, messages, settings(true), Runnable::run, enabled);
    }

    private static NotificationDestination destination(String name, NotificationType channel, String target) {
        NotificationDestination d = new NotificationDestination();
        d.setName(name);
        d.setChannel(channel);
        d.setTarget(target);
        d.setEvents(Set.of(NotificationEvent.LOW_STOCK));
        return d;
    }

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsToEveryDestinationOfTheEventOnItsOwnChannel() {
        when(repository.findEnabledFor(NotificationEvent.LOW_STOCK)).thenReturn(List.of(
                destination("Warehouse", NotificationType.TELEGRAM, "-1001"),
                destination("Owner", NotificationType.TELEGRAM, "-1002"),
                destination("Buyer", NotificationType.EMAIL, "buyer@example.com")));

        service(true).dispatch(NotificationEvent.LOW_STOCK, message);

        assertThat(telegram.sentTo).containsExactly("-1001", "-1002");
        assertThat(email.sentTo).containsExactly("buyer@example.com");
    }

    @Test
    void oneFailingDestinationDoesNotStopTheOthersOrReachTheCaller() {
        telegram.failFor = "-1001";
        telegram.failure = new NotificationException("Telegram refused the message (HTTP 403): bot was kicked");
        when(repository.findEnabledFor(NotificationEvent.LOW_STOCK)).thenReturn(List.of(
                destination("Kicked", NotificationType.TELEGRAM, "-1001"),
                destination("Warehouse", NotificationType.TELEGRAM, "-1002")));

        assertThatCode(() -> service(true).dispatch(NotificationEvent.LOW_STOCK, message)).doesNotThrowAnyException();
        assertThat(telegram.sentTo).containsExactly("-1002");
    }

    @Test
    void unexpectedProviderErrorsAreContainedToo() {
        telegram.failFor = "-1001";
        telegram.failure = new IllegalStateException("I/O error on POST request for https://api.telegram.org/bot123:SECRET");
        when(repository.findEnabledFor(NotificationEvent.LOW_STOCK))
                .thenReturn(List.of(destination("Warehouse", NotificationType.TELEGRAM, "-1001")));

        assertThatCode(() -> service(true).dispatch(NotificationEvent.LOW_STOCK, message)).doesNotThrowAnyException();
    }

    @Test
    void databaseErrorWhileLoadingDestinationsIsContained() {
        when(repository.findEnabledFor(NotificationEvent.LOW_STOCK)).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> service(true).dispatch(NotificationEvent.LOW_STOCK, message)).doesNotThrowAnyException();
    }

    @Test
    void insideATransactionNothingIsSentUntilItCommits() {
        when(repository.findEnabledFor(NotificationEvent.LOW_STOCK))
                .thenReturn(List.of(destination("Warehouse", NotificationType.TELEGRAM, "-1001")));
        TransactionSynchronizationManager.initSynchronization();

        service(true).dispatch(NotificationEvent.LOW_STOCK, message);
        assertThat(telegram.sentTo).isEmpty();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(telegram.sentTo).containsExactly("-1001");
    }

    @Test
    void rolledBackTransactionSendsNothing() {
        TransactionSynchronizationManager.initSynchronization();

        service(true).dispatch(NotificationEvent.LOW_STOCK, message);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(repository);
        assertThat(telegram.sentTo).isEmpty();
    }

    @Test
    void failureAfterCommitDoesNotReachTheCaller() {
        telegram.failFor = "-1001";
        telegram.failure = new NotificationException("Telegram could not be reached (ResourceAccessException)");
        when(repository.findEnabledFor(NotificationEvent.LOW_STOCK))
                .thenReturn(List.of(destination("Warehouse", NotificationType.TELEGRAM, "-1001")));
        TransactionSynchronizationManager.initSynchronization();
        service(true).dispatch(NotificationEvent.LOW_STOCK, message);

        assertThatCode(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit)).doesNotThrowAnyException();
    }

    @Test
    void disabledServiceSendsNothing() {
        service(false).dispatch(NotificationEvent.LOW_STOCK, message);

        verifyNoInteractions(repository);
        assertThat(telegram.sentTo).isEmpty();
    }

    // ----- customer order confirmation --------------------------------------------------------

    private final NotificationMessage adminAlert = new NotificationMessage("🛒 New order ORD-1", "Total: 10.00 USD");
    private final NotificationMessage confirmation = new NotificationMessage("Your Shop order ORD-1 is received", "Thank you");

    private OrderResponse order(String customerEmail) {
        OrderResponse order = OrderResponse.builder()
                .orderNumber("ORD-1")
                .customer(OrderCustomerResponse.builder().name("Dara Sok").email(customerEmail).build())
                .build();
        when(messages.newOrder(order)).thenReturn(adminAlert);
        when(messages.orderConfirmation(order)).thenReturn(confirmation);
        return order;
    }

    @Test
    void newOrderAlertsAdminsAndEmailsTheCustomerAfterCommit() {
        when(repository.findEnabledFor(NotificationEvent.NEW_ORDER))
                .thenReturn(List.of(destination("Sales", NotificationType.TELEGRAM, "-1001")));
        OrderResponse order = order("dara@example.com");
        TransactionSynchronizationManager.initSynchronization();

        service(true).notifyNewOrder(order);
        assertThat(email.sentTo).isEmpty();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(telegram.sentTo).containsExactly("-1001");
        assertThat(email.sentTo).containsExactly("dara@example.com");
        assertThat(email.titles).containsExactly("Your Shop order ORD-1 is received");
    }

    @Test
    void rolledBackOrderEmailsNobody() {
        OrderResponse order = order("dara@example.com");
        TransactionSynchronizationManager.initSynchronization();

        service(true).notifyNewOrder(order);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(email.sentTo).isEmpty();
    }

    @Test
    void customerEmailCanBeSwitchedOffInSettings() {
        OrderResponse order = order("dara@example.com");

        new NotificationService(repository, factory, messages, settings(false), Runnable::run, true).notifyNewOrder(order);

        assertThat(email.sentTo).isEmpty();
    }

    @Test
    void customerWithoutEmailOrStoreWithoutSmtpIsSkipped() {
        service(true).notifyNewOrder(order(null));
        assertThat(email.sentTo).isEmpty();

        email.configured = false;
        service(true).notifyNewOrder(order("dara@example.com"));
        assertThat(email.sentTo).isEmpty();
    }

    @Test
    void failedConfirmationEmailDoesNotReachTheCaller() {
        email.failFor = "dara@example.com";
        email.failure = new NotificationException("Email could not be sent: Connection refused");
        OrderResponse order = order("dara@example.com");

        assertThatCode(() -> service(true).notifyNewOrder(order)).doesNotThrowAnyException();
    }
}
