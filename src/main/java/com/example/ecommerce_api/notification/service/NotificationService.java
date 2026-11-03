package com.example.ecommerce_api.notification.service;

import com.example.ecommerce_api.Enum.NotificationEvent;
import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.notification.entity.NotificationDestination;
import com.example.ecommerce_api.notification.repository.NotificationDestinationRepository;
import com.example.ecommerce_api.notification.strategy.NotificationException;
import com.example.ecommerce_api.notification.strategy.NotificationFactory;
import com.example.ecommerce_api.notification.strategy.NotificationMessage;
import com.example.ecommerce_api.notification.strategy.NotificationStrategy;
import com.example.ecommerce_api.order.dto.OrderResponse;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.settings.service.SettingsService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Sends store events to the destinations admins subscribed to them, through the channel's
 * NotificationStrategy, and order confirmations to customers. Business services only call the
 * notify* methods; they don't know about channels or groups.
 *
 * Delivery happens after the caller's transaction commits (nothing is sent for a rolled-back
 * order) and on a background thread, so a slow or failing Telegram/SMTP server never fails or
 * delays the request. The trade-off: messages wait in memory, so ones not yet sent are lost if
 * the app stops, and a failed send is logged, not retried.
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationDestinationRepository destinationRepository;
    private final NotificationFactory notificationFactory;
    private final NotificationMessages messages;
    private final SettingsService settingsService;
    private final Executor executor;
    private final boolean enabled;
    private final ThreadPoolTaskExecutor ownExecutor;

    @Autowired
    public NotificationService(NotificationDestinationRepository destinationRepository,
                               NotificationFactory notificationFactory,
                               NotificationMessages messages,
                               SettingsService settingsService,
                               @Value("${app.notifications.enabled:true}") boolean enabled) {
        this(destinationRepository, notificationFactory, messages, settingsService, newExecutor(), enabled);
    }

    /** Tests pass a direct executor (Runnable::run). */
    NotificationService(NotificationDestinationRepository destinationRepository,
                        NotificationFactory notificationFactory,
                        NotificationMessages messages,
                        SettingsService settingsService,
                        Executor executor,
                        boolean enabled) {
        this.destinationRepository = destinationRepository;
        this.notificationFactory = notificationFactory;
        this.messages = messages;
        this.settingsService = settingsService;
        this.executor = executor;
        this.enabled = enabled;
        this.ownExecutor = executor instanceof ThreadPoolTaskExecutor pool ? pool : null;
    }

    /** Own small pool rather than a bean, so Spring Boot's default task executor stays as it is. */
    private static ThreadPoolTaskExecutor newExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("notify-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        // On shutdown, give queued messages a moment to go out
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @PreDestroy
    void shutdown() {
        if (ownExecutor != null) {
            ownExecutor.shutdown();
        }
    }

    // ----- store events -----------------------------------------------------------------------

    /**
     * Alerts the NEW_ORDER destinations and, while Settings -> Notifications has it switched on,
     * emails the customer an order confirmation.
     */
    public void notifyNewOrder(OrderResponse order) {
        dispatch(NotificationEvent.NEW_ORDER, messages.newOrder(order));

        String customerEmail = order.getCustomer() != null ? order.getCustomer().getEmail() : null;
        if (StringUtils.hasText(customerEmail) && settingsService.get().getNotifications().isCustomerOrderEmail()) {
            NotificationMessage confirmation = messages.orderConfirmation(order);
            afterCommit(() -> deliverToCustomer(order.getOrderNumber(), customerEmail, confirmation),
                    "order confirmation " + order.getOrderNumber());
        }
    }

    public void notifyLowStock(String productTitle, String sku, int available, int threshold) {
        dispatch(NotificationEvent.LOW_STOCK, messages.lowStock(productTitle, sku, available, threshold));
    }

    public void notifyNewProduct(ProductResponse product) {
        dispatch(NotificationEvent.NEW_PRODUCT, messages.newProduct(product));
    }

    /** Admin "send test": synchronous, so the admin sees why it failed (e.g. "chat not found"). */
    public void sendTest(NotificationDestination destination) {
        NotificationMessage message = new NotificationMessage("✅ Test notification",
                "\"" + destination.getName() + "\" is set up. It receives: " + destination.getEvents() + ".");
        notificationFactory.get(destination.getChannel()).send(destination.getTarget(), message);
    }

    // ----- delivery ---------------------------------------------------------------------------

    /**
     * The message is built by the caller, inside its transaction, while entities can still be
     * read; only sending is deferred.
     */
    void dispatch(NotificationEvent event, NotificationMessage message) {
        afterCommit(() -> deliver(event, message), event + " \"" + message.title() + "\"");
    }

    /** Runs the delivery on the notification thread once the current transaction commits (right away without one). */
    private void afterCommit(Runnable delivery, String description) {
        if (!enabled) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Must not throw: an exception here reaches the caller after the data is committed
                    submit(delivery, description);
                }
            });
        } else {
            submit(delivery, description);
        }
    }

    private void submit(Runnable delivery, String description) {
        try {
            executor.execute(delivery);
        } catch (RejectedExecutionException e) {
            log.warn("Notification queue is full; dropped {}", description);
        }
    }

    /** Sends to every enabled destination of the event; one failing destination doesn't stop the others. */
    void deliver(NotificationEvent event, NotificationMessage message) {
        List<NotificationDestination> destinations;
        try {
            destinations = destinationRepository.findEnabledFor(event);
        } catch (RuntimeException e) {
            log.warn("Could not load notification destinations for {}: {}", event, e.getClass().getSimpleName());
            return;
        }

        for (NotificationDestination destination : destinations) {
            try {
                notificationFactory.get(destination.getChannel()).send(destination.getTarget(), message);
                log.debug("Sent {} to \"{}\" ({})", event, destination.getName(), destination.getChannel());
            } catch (NotificationException e) {
                log.warn("{} notification to \"{}\" ({}) failed: {}",
                        event, destination.getName(), destination.getChannel(), e.getMessage());
            } catch (RuntimeException e) {
                // Only the type: other exceptions' messages may contain a request URL with the bot token
                log.warn("{} notification to \"{}\" ({}) failed: {}",
                        event, destination.getName(), destination.getChannel(), e.getClass().getSimpleName());
            }
        }
    }

    /**
     * Logs the order number, not the customer's address. Skipped quietly while email isn't
     * configured, so stores without SMTP don't get a warning for every order.
     */
    void deliverToCustomer(String orderNumber, String customerEmail, NotificationMessage message) {
        try {
            NotificationStrategy email = notificationFactory.get(NotificationType.EMAIL);
            if (!email.isConfigured()) {
                log.debug("Email is not configured; no confirmation sent for order {}", orderNumber);
                return;
            }
            email.send(customerEmail, message);
            log.info("Order confirmation for {} sent to the customer", orderNumber);
        } catch (NotificationException e) {
            log.warn("Order confirmation for {} failed: {}", orderNumber, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Order confirmation for {} failed: {}", orderNumber, e.getClass().getSimpleName());
        }
    }
}
