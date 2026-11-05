package com.example.ecommerce_api.payment.service;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import com.example.ecommerce_api.payment.entity.Payment;
import com.example.ecommerce_api.payment.gateway.AbaPayWayGateway;
import com.example.ecommerce_api.payment.gateway.GatewayEvent;
import com.example.ecommerce_api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** The ABA PayWay pieces that sit outside the PaymentGateway contract: the checkout page and the callback. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("!'${app.aba-payway.merchant-id:}'.isBlank() and !'${app.aba-payway.public-key:}'.isBlank()")
public class AbaPayWayService {

    private final AbaPayWayGateway gateway;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * The page that sends the customer to PayWay, or empty when the link is forged (wrong signature)
     * or the attempt is no longer pending (paid, cancelled, replaced by another method).
     */
    @Transactional(readOnly = true)
    public Optional<String> checkoutPage(Long paymentId, String signature) {
        return paymentRepository.findById(paymentId)
                .filter(payment -> payment.getMethod() == PaymentMethod.ABA_PAYWAY)
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .filter(payment -> gateway.isValidCheckoutSignature(payment, signature))
                .map(payment -> gateway.checkoutPage(payment.getOrder(), payment));
    }

    /**
     * PayWay's push after a payment. Anyone can post here, so the body is only used to know which
     * transaction to look at; what happened is asked from PayWay itself.
     */
    public void handleCallback(String tranId) {
        Optional<Payment> payment = paymentRepository.findByMethodAndProviderReference(PaymentMethod.ABA_PAYWAY, tranId)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING);
        if (payment.isEmpty()) {
            log.info("ABA PayWay callback for {} ignored: no pending payment", tranId);
            return;
        }
        GatewayEvent event = gateway.checkStatus(payment.get());
        if (event != null) {
            paymentService.handleGatewayEvent(PaymentMethod.ABA_PAYWAY, event);
        }
    }
}
