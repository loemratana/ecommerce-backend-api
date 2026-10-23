package com.example.ecommerce_api.payment.service;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.common.exception.ResourceNotFoundException;
import com.example.ecommerce_api.common.response.PageResponse;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.payment.dto.InitiatePaymentRequest;
import com.example.ecommerce_api.payment.dto.PaymentResponse;
import com.example.ecommerce_api.payment.entity.Payment;
import com.example.ecommerce_api.payment.entity.ProcessedPaymentEvent;
import com.example.ecommerce_api.payment.gateway.GatewayEvent;
import com.example.ecommerce_api.payment.gateway.PaymentGateway;
import com.example.ecommerce_api.payment.gateway.PaymentGatewayResolver;
import com.example.ecommerce_api.payment.gateway.PaymentInitResult;
import com.example.ecommerce_api.payment.gateway.RefundResult;
import com.example.ecommerce_api.payment.mapper.PaymentMapper;
import com.example.ecommerce_api.payment.repository.PaymentRepository;
import com.example.ecommerce_api.payment.repository.ProcessedPaymentEventRepository;
import com.example.ecommerce_api.security.SecurityUtils;
import com.example.ecommerce_api.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Rules shared by every gateway. Lock order is always order -> payment, so a gateway status check
 * and a customer action on the same order can't deadlock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final ProcessedPaymentEventRepository processedEventRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayResolver gatewayResolver;
    private final PaymentMapper paymentMapper;
    private final SecurityUtils securityUtils;
    private final SettingsService settingsService;

    @Override
    @Transactional(readOnly = true)
    public Set<PaymentMethod> availableMethods() {
        return gatewayResolver.availableMethods();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyOrderPayments(String orderNumber) {
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, securityUtils.getCurrentUserId())
                .orElseThrow(() -> orderNotFound(orderNumber));
        return toResponses(order);
    }

    @Override
    public List<PaymentResponse> confirmMyOrderPayments(String orderNumber) {
        Long userId = securityUtils.getCurrentUserId();
        // Locked so two confirmations at once (double reload) apply the result only once.
        Order order = orderRepository.lockByOrderNumber(orderNumber)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> orderNotFound(orderNumber));

        for (Payment pending : paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING))
        {
            GatewayEvent event = gatewayResolver.get(pending.getMethod()).checkStatus(pending);
            if (event != null)
            {
                handleGatewayEvent(pending.getMethod(), event);
            }
        }
        return toResponses(order);
    }

    @Override
    public PaymentResponse initiate(InitiatePaymentRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        String orderNumber = request.getOrderNumber();
        Order order = orderRepository.lockByOrderNumber(orderNumber)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> orderNotFound(orderNumber));

        if (order.getStatus() != OrderStatus.PENDING)
        {
            throw new BusinessException("Order " + orderNumber + " is " + order.getStatus() + "; only pending orders can be paid");
        }

        PaymentGateway gateway = gatewayResolver.get(request.getMethod());
        for (Payment open : paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING))
        {
            if (open.getMethod() == request.getMethod())
            {
                return paymentMapper.toResponse(open);   // retry / double click: reuse the open attempt
            }
            cancelPending(open);                         // customer switched method
        }

        // The amount always comes from the order, never from the client.
        String currency = settingsService.get().getLocalization().getCurrency();
        Payment payment = paymentRepository.save(new Payment(order, request.getMethod(), order.getTotalOrderPrice(), currency));
        PaymentInitResult result = gateway.initiate(order, payment);
        payment.attachProvider(result.providerReference(), result.redirectUrl());
        order.choosePaymentMethod(request.getMethod());
        log.info("Started {} payment {} for order {} ({} {})", payment.getMethod(), payment.getId(), orderNumber, payment.getAmount(), currency);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getAll(PaymentStatus status, PaymentMethod method, Pageable pageable) {
        return new PageResponse<>(paymentRepository.search(status, method, pageable).map(paymentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getOrderPayments(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> orderNotFound(orderNumber));
        return toResponses(order);
    }

    @Override
    public void handleGatewayEvent(PaymentMethod method, GatewayEvent event) {

        if (event.type() == GatewayEvent.Type.IGNORED)
        {
            return;
        }

        if (processedEventRepository.existsByMethodAndEventId(method, event.eventId()))
        {
            log.info("Skipping {} event {}: already processed", method, event.eventId());
            return;
        }
        // Same transaction: if handling fails this rolls back too, and the next check runs it again.
        // Two deliveries at once both pass the check above; the unique constraint fails one of them.
        processedEventRepository.save(new ProcessedPaymentEvent(method, event.eventId()));

        String orderNumber = paymentRepository.findOrderNumberByProviderReference(method, event.providerReference())
                .orElse(null);
        if (orderNumber == null)
        {
            log.warn("{} event {} refers to unknown payment {}", method, event.eventId(), event.providerReference());
            return;
        }
        Order order = orderRepository.lockByOrderNumber(orderNumber).orElseThrow(() -> orderNotFound(orderNumber));
        Payment payment = paymentRepository.lockByProviderReference(method, event.providerReference()).orElseThrow();

        switch (event.type()) {
            case PAYMENT_SUCCEEDED -> paymentSucceeded(payment, order, event.amount());
            case PAYMENT_FAILED -> {
                if (payment.getStatus() == PaymentStatus.PENDING) payment.markFailed(event.failureReason());
            }
            case PAYMENT_EXPIRED -> {
                if (payment.getStatus() == PaymentStatus.PENDING) payment.markExpired();
            }
            case REFUNDED -> refundedAtGateway(payment, order, event.amount());
            case IGNORED -> { }
        }
    }

    @Override
    public void onOrderCancelled(Order order) {
        BigDecimal captured = BigDecimal.ZERO;
        for (Payment payment : paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()))
        {
            if (payment.getStatus() == PaymentStatus.PENDING)
            {
                cancelPending(payment);
            }
            else
            {
                captured = captured.add(payment.getRefundableAmount());
            }
        }
        // A paid order was cancelled (only possible before shipping): give all of it back.
        if (captured.signum() > 0)
        {
            refund(order, captured, "Order " + order.getOrderNumber() + " cancelled");
        }
    }

    @Override
    public BigDecimal refund(Order order, BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0)
        {
            return BigDecimal.ZERO;
        }
        BigDecimal remaining = amount;
        for (Payment payment : paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()))
        {
            BigDecimal part = payment.getRefundableAmount().min(remaining);
            if (part.signum() > 0)
            {
                refundPayment(payment, part, reason);
                remaining = remaining.subtract(part);
            }
            if (remaining.signum() == 0) break;
        }
        if (remaining.signum() > 0)
        {
            log.warn("{}: {} of {} could not be refunded through a payment on order {} (no captured payment); refund it manually",
                    reason, remaining, amount, order.getOrderNumber());
        }
        return amount.subtract(remaining);
    }

    @Override
    public void onOrderDelivered(Order order) {
        for (Payment payment : paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING))
        {
            if (payment.getMethod() == PaymentMethod.CASH_ON_DELIVERY)
            {
                payment.markSucceeded();
                log.info("Cash collected for order {} (payment {})", order.getOrderNumber(), payment.getId());
            }
        }
    }

    // ----- helpers ----------------------------------------------------------------------------

    private void paymentSucceeded(Payment payment, Order order, BigDecimal amount)
    {
        if (payment.getStatus() != PaymentStatus.PENDING)
        {
            log.error("Payment {} succeeded at the gateway but is {} here; needs review", payment.getId(), payment.getStatus());
            return;
        }
        if (amount != null && amount.compareTo(payment.getAmount()) != 0)
        {
            payment.markFailed("Gateway reported " + amount + " but " + payment.getAmount() + " was expected");
            log.error("Amount mismatch on payment {}; needs review", payment.getId());
            return;
        }
        payment.markSucceeded();
        if (order.getStatus() == OrderStatus.PENDING)
        {
            order.markPaid();
        }
        else
        {
            // e.g. the order expired while the customer was on the payment page: the stock is gone,
            // so the money goes straight back.
            log.warn("Payment {} succeeded for order {} which is {}; refunding it",
                    payment.getId(), order.getOrderNumber(), order.getStatus());
            refundPayment(payment, payment.getRefundableAmount(),
                    "Paid after order " + order.getOrderNumber() + " was " + order.getStatus());
        }
    }

    /**
     * The gateway reports {@code totalRefunded} on this payment so far. Our own refunds are already
     * recorded (the order lock makes this wait until they commit), so only the difference is new:
     * a refund made directly in the provider's dashboard.
     */
    private void refundedAtGateway(Payment payment, Order order, BigDecimal totalRefunded)
    {
        if (totalRefunded == null) return;
        BigDecimal extra = totalRefunded.subtract(payment.getRefundedAmount()).min(payment.getRefundableAmount());
        if (extra.signum() <= 0) return;   // nothing new: an echo of a refund we made

        payment.recordRefund(extra);
        log.info("Payment {} refunded {} {} outside the store (gateway dashboard)", payment.getId(), extra, payment.getCurrency());

        if (order.getStatus() == OrderStatus.DELIVERED)
        {
            // Counts against revenue like a return; capped at what the order allows (shipping excluded).
            BigDecimal onOrder = extra.min(order.getRefundableAmount());
            if (onOrder.signum() > 0) order.recordRefund(onOrder);
        }
        else if (order.getStatus() != OrderStatus.CANCELLED && payment.getStatus() == PaymentStatus.REFUNDED)
        {
            log.warn("Order {} was fully refunded at the gateway but is still {}; cancel it if it shouldn't ship",
                    order.getOrderNumber(), order.getStatus());
        }
    }

    private void cancelPending(Payment payment)
    {
        gatewayResolver.get(payment.getMethod()).cancel(payment);
        payment.markCancelled();
    }

    /** Gateway first: if it refuses, it throws and nothing is recorded. */
    private void refundPayment(Payment payment, BigDecimal amount, String reason)
    {
        RefundResult result = gatewayResolver.get(payment.getMethod()).refund(payment, amount);
        payment.recordRefund(amount);
        if (result.manual())
        {
            log.warn("{}: hand back {} {} for payment {} ({}) manually", reason, amount, payment.getCurrency(),
                    payment.getId(), payment.getMethod());
        }
        else
        {
            log.info("{}: refunded {} {} on payment {} ({} refund {})", reason, amount, payment.getCurrency(),
                    payment.getId(), payment.getMethod(), result.providerRefundId());
        }
    }

    private List<PaymentResponse> toResponses(Order order) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    private static ResourceNotFoundException orderNotFound(String orderNumber)
    {
        return new ResourceNotFoundException("Order not found: " + orderNumber);
    }
}
