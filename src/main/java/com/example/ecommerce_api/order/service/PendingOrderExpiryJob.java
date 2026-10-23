package com.example.ecommerce_api.order.service;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Unpaid orders reserve stock at checkout. Without a time limit, abandoned checkouts would keep
 * products looking sold out forever, so PENDING orders older than the timeout are cancelled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderExpiryJob {

    private static final int BATCH_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final SettingsService settingsService;

    @Scheduled(fixedDelayString = "${app.order.expiry-check-interval:PT1M}")
    public void expireUnpaidOrders() {
        Duration pendingTimeout = Duration.ofMinutes(settingsService.get().getOrders().getPendingTimeoutMinutes());
        LocalDateTime cutoff = LocalDateTime.now().minus(pendingTimeout);
        List<String> orderNumbers = orderRepository.findOrderNumbersByStatusPlacedBefore(
                OrderStatus.PENDING, cutoff, PageRequest.of(0, BATCH_SIZE));

        int expired = 0;
        for (String orderNumber : orderNumbers) {
            // One transaction per order, so one failure doesn't roll back the others.
            try {
                if (orderService.expireIfStillPending(orderNumber)) expired++;
            } catch (RuntimeException e) {
                log.error("Could not expire order {}", orderNumber, e);
            }
        }
        if (expired > 0) {
            log.info("Expired {} unpaid order(s) older than {}", expired, pendingTimeout);
        }
    }
}
