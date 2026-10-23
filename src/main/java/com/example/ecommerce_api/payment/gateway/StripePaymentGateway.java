package com.example.ecommerce_api.payment.gateway;


import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.payment.entity.Payment;
import com.example.ecommerce_api.settings.service.SettingsService;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Stripe hosted Checkout, without webhooks. The customer pays on Stripe's page; the order is only
 * marked paid after we retrieve the session from Stripe and it says "paid" (the success redirect
 * alone can be faked). The storefront asks for that check via PaymentService.confirmMyOrderPayments.
 *
 * providerReference = Checkout Session id. Created only when app.stripe.secret-key is set.
 */
@Slf4j
@Component
@ConditionalOnExpression("!'${app.stripe.secret-key:}'.isBlank()")
public class StripePaymentGateway implements  PaymentGateway{

    // Stripe's limits for a Checkout Session's expires_at, minus a minute of margin each side
    private static  final Duration MIN_SESSION_LIFETIME  = Duration.ofMinutes(31);
    private static final  Duration MAX_SESSION_LIFETIME = Duration.ofHours(23).plusMinutes(59);
    private final String secretKey;
    private final String storefrontUrl;
    private final SettingsService settingsService;

    public StripePaymentGateway(
            @Value("${app.stripe.secret-key}") String secretKey,
            @Value("${app.stripe.storefront-url}") String storefrontUrl,
            SettingsService settingsService
    )
    {
        this.secretKey = secretKey;
        this.storefrontUrl = storefrontUrl.replaceAll("/+$", "");
        this.settingsService = settingsService;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.STRIPE;
    }

    @Override
    public PaymentInitResult initiate(Order order, Payment payment) {
        String orderNumber = order.getOrderNumber();
        String orderParam  = URLEncoder.encode(orderNumber, StandardCharsets.UTF_8);

        // One line for the whole order total, so Stripe charges exactly what the order says
        // (items, coupons, tax and shipping are already in it).
        SessionCreateParams.LineItem line = SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(payment.getCurrency().toLowerCase())
                        .setUnitAmount(toMinorUnits(payment.getAmount(), payment.getCurrency()))
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName("Order " + orderNumber)
                                .build())
                        .build())
                .build();

        SessionCreateParams.Builder params  = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(storefrontUrl + "/checkout/success?order=" + orderParam)
                .setCancelUrl(storefrontUrl + "/checkout/cancel?order=" + orderParam)
                .setClientReferenceId(orderNumber)
                .setExpiresAt(sessionExpiry(order).getEpochSecond())
                .addLineItem(line)
                .putMetadata("orderNumber", orderNumber)
                .putMetadata("paymentId", String.valueOf(payment.getId()))
                // Also on the PaymentIntent, so it's visible on the charge in the Stripe dashboard
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("orderNumber", orderNumber)
                        .putMetadata("paymentId", String.valueOf(payment.getId()))
                        .build()
                );
        if (order.getUser() != null && order.getUser().getEmail() != null)
        {
            params.setCustomerEmail(order.getUser().getEmail());
        }
        try {
            // Idempotency key: a retry of this request can't create a second session for the same attempt
            Session session = Session.create(params.build(), options("checkout-session-" + payment.getId()));
            log.info("Created Stripe Checkout Session {} for order {}", session.getId(), orderNumber);
            return new PaymentInitResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe rejected the Checkout Session for order {}: {}", orderNumber, e.getMessage());
            throw new BusinessException("Card payment is unavailable right now. Please try again or choose another method.");
        }
    }

    @Override
    public void cancel(Payment payment) {
        if (payment.getProviderReference() == null) {
            return;
        }
        try {
            Session session = Session.retrieve(payment.getProviderReference(), options(null));
            if ("open".equals(session.getStatus())) {
                session.expire(options(null));
            }
        } catch (StripeException e) {
            // Don't block cancelling the order on a Stripe outage. If the customer still pays,
            // PaymentServiceImpl logs "succeeded ... which is CANCELLED; it must be refunded".
            log.warn("Could not expire Stripe session {}: {}", payment.getProviderReference(), e.getMessage());
        }
    }

    @Override
    public GatewayEvent checkStatus(Payment payment) {
        if (payment.getProviderReference() == null) {
            return null;
        }
        Session session;
        try {
            session = Session.retrieve(payment.getProviderReference(), options(null));
        } catch (StripeException e) {
            log.warn("Could not check Stripe session {}: {}", payment.getProviderReference(), e.getMessage());
            return null;
        }
        // The event id only has to be unique per outcome: a session is paid or expires once.
        if ("paid".equals(session.getPaymentStatus())) {
            return new GatewayEvent("paid:" + session.getId(), GatewayEvent.Type.PAYMENT_SUCCEEDED, session.getId(),
                    fromMinorUnits(session.getAmountTotal(), session.getCurrency()), null);
        }
        if ("expired".equals(session.getStatus())) {
            return new GatewayEvent("expired:" + session.getId(), GatewayEvent.Type.PAYMENT_EXPIRED, session.getId(), null, null);
        }
        return null;   // still open, or a delayed method (bank debit) that hasn't cleared yet
    }

    @Override
    public RefundResult refund(Payment payment, BigDecimal amount) {
        try {
            String paymentIntent = Session.retrieve(payment.getProviderReference(), options(null)).getPaymentIntent();
            Refund refund = Refund.create(RefundCreateParams.builder()
                            .setPaymentIntent(paymentIntent)
                            .setAmount(toMinorUnits(amount, payment.getCurrency()))
                            .putMetadata("paymentId", String.valueOf(payment.getId()))
                            .build(),
                    // Keyed on what was refunded before, so retrying the same refund can't refund twice
                    options("refund-" + payment.getId() + "-" + payment.getRefundedAmount().toPlainString()));
            return new RefundResult(refund.getId(), amount, false);
        } catch (StripeException e) {
            log.error("Stripe refund failed for payment {}: {}", payment.getId(), e.getMessage());
            throw new BusinessException("The refund could not be processed by Stripe: " + e.getUserMessage());
        }
    }

    // ----- helpers ----------------------------------------------------------------------------

    /**
     * The session should end with the order's reservation (placed + pending timeout), but Stripe only
     * allows 30 minutes to 24 hours. With a pending timeout under 31 minutes, the session outlives the
     * order; a late payment is then caught by PaymentServiceImpl as "must be refunded".
     */
    private Instant sessionExpiry(Order order)
    {
        int timeoutMinutes = settingsService.get().getOrders().getPendingTimeoutMinutes();
        Instant orderExpires  = order.getOrderDate().atZone(ZoneId.systemDefault()).toInstant()
                .plus(Duration.ofMinutes(timeoutMinutes));

        Instant now = Instant.now();
        Instant min = now.plus(MIN_SESSION_LIFETIME);
        Instant max = now.plus(MAX_SESSION_LIFETIME);

        return orderExpires.isBefore(min) ? min : orderExpires.isAfter(max) ? max : orderExpires;
    }

    private RequestOptions options(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder().setApiKey(secretKey);
        if (idempotencyKey != null) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }

    /** 12.50 USD -> 1250; zero-decimal currencies (e.g. JPY) stay as is. */
    private static long toMinorUnits(BigDecimal amount , String currency)
    {
        int digits = Math.max(0, Currency.getInstance(currency).getDefaultFractionDigits());
        return amount.movePointRight(digits).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static BigDecimal fromMinorUnits(Long amount, String currency)
    {
        if (amount == null) return null;
        int digits = Math.max(0, Currency.getInstance(currency.toUpperCase()).getDefaultFractionDigits());
        return BigDecimal.valueOf(amount).movePointLeft(digits);
    }
}
