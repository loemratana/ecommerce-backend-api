package com.example.ecommerce_api.payment.service;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.payment.gateway.PaymentGatewayResolver;
import com.example.ecommerce_api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Online payments are confirmed when the customer comes back to the shop or when the gateway calls
 * our callback. Neither is guaranteed: a KHQR payment is made in the ABA app, the customer may close
 * the browser, and the PayWay sandbox can't reach a callback on localhost. So orders still waiting
 * for an online payment are checked with the gateway on a timer, and paid ones are marked PAID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentSyncJob {

    private static final int BATCH_SIZE = 50;

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PaymentGatewayResolver gatewayResolver;

    @Scheduled(fixedDelayString = "${app.payment.sync-interval:PT30S}", initialDelayString = "PT30S")
    public void syncPendingPayments() {
        // Cash on delivery is settled by the courier, not by a gateway
        Set<PaymentMethod> online = EnumSet.noneOf(PaymentMethod.class);
        online.addAll(gatewayResolver.availableMethods());
        online.remove(PaymentMethod.CASH_ON_DELIVERY);
        if (online.isEmpty()) {
            return;
        }

        List<String> orderNumbers = paymentRepository.findOrderNumbersAwaitingGateway(online, PageRequest.of(0, BATCH_SIZE));
        for (String orderNumber : orderNumbers) {
            // One transaction per order, so one gateway error doesn't stop the others.
            try {
                paymentService.syncPendingPayments(orderNumber);
            } catch (RuntimeException e) {
                log.warn("Could not sync payments of order {}: {}", orderNumber, e.getMessage());
            }
        }
    }
}
