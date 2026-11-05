package com.example.ecommerce_api.payment.gateway;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.payment.entity.Payment;
import com.example.ecommerce_api.settings.service.SettingsService;
import com.example.ecommerce_api.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ABA PayWay hosted checkout (cards, ABA Pay, KHQR), without trusting the browser. PayWay's purchase
 * API is a signed form POST that answers with PayWay's checkout page, which has to load in the
 * customer's browser, so the redirect URL points at our own checkout page (AbaPayWayController): it
 * signs the form at that moment and submits it to PayWay. The order is only marked paid after PayWay's
 * check-transaction API says APPROVED: when the customer returns (PaymentService.confirmMyOrderPayments)
 * or when PayWay calls our callback.
 *
 * providerReference = PayWay tran_id. Created only when app.aba-payway.merchant-id and public-key are set.
 * Refunds are manual (PayWay merchant portal): PayWay's refund API needs RSA keys issued by ABA.
 */
@Slf4j
@Component
@ConditionalOnExpression("!'${app.aba-payway.merchant-id:}'.isBlank() and !'${app.aba-payway.public-key:}'.isBlank()")
public class AbaPayWayGateway implements PaymentGateway {

    public static final String CHECKOUT_PATH = "/api/v1/payments/aba-payway/checkout/";

    private static final String PURCHASE_PATH = "/api/payment-gateway/v1/payments/purchase";
    private static final String CHECK_TRANSACTION_PATH = "/api/payment-gateway/v1/payments/check-transaction-2";
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "KHR");
    private static final DateTimeFormatter REQ_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    // PayWay's limits for a payment's lifetime, in minutes
    private static final long MIN_LIFETIME_MINUTES = 3;
    private static final long MAX_LIFETIME_MINUTES = 30L * 24 * 60;

    private final String merchantId;
    private final String publicKey;
    private final String baseUrl;
    private final String callbackUrl;
    private final String storefrontUrl;
    private final String paymentOption;
    private final SettingsService settingsService;
    private final RestClient restClient;

    public AbaPayWayGateway(
            @Value("${app.aba-payway.merchant-id}") String merchantId,
            @Value("${app.aba-payway.public-key}") String publicKey,
            @Value("${app.aba-payway.base-url}") String baseUrl,
            @Value("${app.aba-payway.callback-url:}") String callbackUrl,
            @Value("${app.aba-payway.storefront-url}") String storefrontUrl,
            @Value("${app.aba-payway.payment-option:}") String paymentOption,
            SettingsService settingsService
    ) {
        this.merchantId = merchantId.trim();
        this.publicKey = publicKey.trim();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.callbackUrl = callbackUrl.trim();
        this.storefrontUrl = storefrontUrl.replaceAll("/+$", "");
        this.paymentOption = paymentOption.trim();
        this.settingsService = settingsService;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(requestFactory)
                // PayWay sits behind Cloudflare, which blocks some default client user agents (error 1010)
                .defaultHeader(HttpHeaders.USER_AGENT, "ecommerce-api/1.0")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.ABA_PAYWAY;
    }

    /**
     * Nothing is sent to PayWay yet: the signed form is built when the customer opens the checkout
     * page, so its req_time is fresh. Only the tran_id is fixed here.
     */
    @Override
    public PaymentInitResult initiate(Order order, Payment payment) {
        if (!SUPPORTED_CURRENCIES.contains(payment.getCurrency())) {
            throw new BusinessException("ABA PayWay only accepts USD or KHR; the store currency is " + payment.getCurrency());
        }
        String tranId = newTranId(payment);
        String checkoutUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(CHECKOUT_PATH + payment.getId())
                .queryParam("sig", checkoutSignature(payment.getId(), tranId))
                .toUriString();
        log.info("Prepared ABA PayWay transaction {} for order {}", tranId, order.getOrderNumber());
        return new PaymentInitResult(tranId, checkoutUrl);
    }

    /**
     * There is no call to close an open PayWay transaction, so cancelling only stops our checkout page
     * from opening it again (it refuses non-pending payments); the lifetime ends it at PayWay. If the
     * customer still pays, PaymentServiceImpl logs that the money must be handed back.
     */
    @Override
    public void cancel(Payment payment) {
    }

    @Override
    public GatewayEvent checkStatus(Payment payment) {
        String tranId = payment.getProviderReference();
        if (tranId == null) {
            return null;
        }
        String reqTime = reqTime();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("req_time", reqTime);
        body.put("merchant_id", merchantId);
        body.put("tran_id", tranId);
        body.put("hash", hmacSha512Base64(reqTime + merchantId + tranId));

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(CHECK_TRANSACTION_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            log.warn("Could not check ABA PayWay transaction {}: {}", tranId, e.getMessage());
            return null;
        }
        if (response == null) {
            return null;
        }

        Map<?, ?> status = response.get("status") instanceof Map<?, ?> map ? map : Map.of();
        String code = String.valueOf(status.get("code"));
        if (!"00".equals(code)) {
            if ("6".equals(code)) {
                // "Transaction not found": the customer hasn't opened PayWay's checkout yet
                log.debug("ABA PayWay has no transaction {} yet", tranId);
            } else {
                // e.g. a wrong hash / merchant id: the order would stay PENDING forever, so say why
                log.warn("ABA PayWay check-transaction for {} answered {} {}", tranId, code, status.get("message"));
            }
            return null;
        }
        Map<?, ?> data = response.get("data") instanceof Map<?, ?> map ? map : Map.of();
        String paymentStatus = String.valueOf(data.get("payment_status")).trim().toUpperCase(java.util.Locale.ROOT);
        log.debug("ABA PayWay transaction {} is {} ({})", tranId, paymentStatus, data);

        // The event id only has to be unique per outcome: a transaction is approved or declined once.
        return switch (paymentStatus) {
            case "APPROVED" -> {
                // PayWay leaves payment_currency empty in some states; only a different currency is a mismatch
                String paidCurrency = data.get("payment_currency") == null ? "" : String.valueOf(data.get("payment_currency")).trim();
                if (!paidCurrency.isEmpty() && !payment.getCurrency().equalsIgnoreCase(paidCurrency)) {
                    yield new GatewayEvent("approved:" + tranId, GatewayEvent.Type.PAYMENT_FAILED, tranId, null,
                            "ABA PayWay charged " + paidCurrency + " but " + payment.getCurrency() + " was expected");
                }
                Object amount = data.get("original_amount") != null ? data.get("original_amount") : data.get("total_amount");
                yield new GatewayEvent("approved:" + tranId, GatewayEvent.Type.PAYMENT_SUCCEEDED, tranId,
                        amount == null ? null : new BigDecimal(String.valueOf(amount)), null);
            }
            case "DECLINED" -> new GatewayEvent("declined:" + tranId, GatewayEvent.Type.PAYMENT_FAILED, tranId, null,
                    "Declined by ABA PayWay");
            case "CANCELLED" -> new GatewayEvent("cancelled:" + tranId, GatewayEvent.Type.PAYMENT_FAILED, tranId, null,
                    "Cancelled at ABA PayWay");
            default -> null;   // PENDING / PRE-AUTH: not paid yet
        };
    }

    @Override
    public RefundResult refund(Payment payment, BigDecimal amount) {
        return new RefundResult(null, amount, true);
    }

    // ----- checkout page ----------------------------------------------------------------------

    /** Whether {@code signature} is the one initiate() put in this payment's checkout URL. */
    public boolean isValidCheckoutSignature(Payment payment, String signature) {
        if (signature == null || payment.getProviderReference() == null) {
            return false;
        }
        byte[] expected = checkoutSignature(payment.getId(), payment.getProviderReference()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    /** An HTML page that posts the signed purchase form to PayWay as soon as it loads. */
    public String checkoutPage(Order order, Payment payment) {
        Map<String, String> fields = purchaseFields(order, payment);
        StringBuilder inputs = new StringBuilder();
        fields.forEach((name, value) -> inputs.append("<input type=\"hidden\" name=\"").append(name)
                .append("\" value=\"").append(HtmlUtils.htmlEscape(value)).append("\">\n"));

        return """
                <!doctype html>
                <html lang="en">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Redirecting to ABA PayWay</title></head>
                <body style="font-family: system-ui, sans-serif; text-align: center; padding: 48px 16px;">
                <p>Redirecting you to ABA PayWay&hellip;</p>
                <form id="payway" method="post" action="%s">
                %s<noscript><button type="submit">Continue to ABA PayWay</button></noscript>
                </form>
                <script>document.getElementById("payway").submit();</script>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(baseUrl + PURCHASE_PATH), inputs);
    }

    /**
     * The purchase form. Optional fields we don't use (items, shipping, ...) are left out of the form
     * and count as "" in the hash, whose field order is fixed by PayWay.
     */
    Map<String, String> purchaseFields(Order order, Payment payment) {
        User user = order.getUser();
        String orderParam = URLEncoder.encode(order.getOrderNumber(), StandardCharsets.UTF_8);

        String reqTime = reqTime();
        String tranId = payment.getProviderReference();
        String amount = formatAmount(payment.getAmount(), payment.getCurrency());
        String firstName = nullToEmpty(user == null ? null : user.getFirstName());
        String lastName = nullToEmpty(user == null ? null : user.getLastName());
        String email = nullToEmpty(user == null ? null : user.getEmail());
        String phone = nullToEmpty(user == null ? null : user.getPhone());
        String type = "purchase";
        // return_url is where PayWay pushes the result server-to-server; PayWay wants it base64 encoded
        String returnUrl = callbackUrl.isEmpty() ? "" : Base64.getEncoder().encodeToString(callbackUrl.getBytes(StandardCharsets.UTF_8));
        String cancelUrl = storefrontUrl + "/checkout/cancel?order=" + orderParam;
        String continueSuccessUrl = storefrontUrl + "/checkout/success?order=" + orderParam;
        String currency = payment.getCurrency();
        String lifetime = String.valueOf(lifetimeMinutes(order));

        String hash = hmacSha512Base64(reqTime + merchantId + tranId + amount
                + /* items */ "" + /* shipping */ ""
                + firstName + lastName + email + phone + type + paymentOption
                + returnUrl + cancelUrl + continueSuccessUrl + /* return_deeplink */ ""
                + currency + /* custom_fields */ "" + /* return_params */ "" + /* payout */ ""
                + lifetime);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("req_time", reqTime);
        fields.put("merchant_id", merchantId);
        fields.put("tran_id", tranId);
        fields.put("amount", amount);
        fields.put("firstname", firstName);
        fields.put("lastname", lastName);
        fields.put("email", email);
        fields.put("phone", phone);
        fields.put("type", type);
        if (!paymentOption.isEmpty()) fields.put("payment_option", paymentOption);
        if (!returnUrl.isEmpty()) fields.put("return_url", returnUrl);
        fields.put("cancel_url", cancelUrl);
        fields.put("continue_success_url", continueSuccessUrl);
        fields.put("currency", currency);
        fields.put("lifetime", lifetime);
        // Our profile also has PayWay's QR Payment API; 0 = use the hosted Checkout (not part of the hash)
        fields.put("payment_gate", "0");
        fields.put("hash", hash);
        return fields;
    }

    // ----- helpers ----------------------------------------------------------------------------

    /** Unique per attempt and across database resets (PayWay rejects a reused tran_id); at most 20 characters. */
    private static String newTranId(Payment payment) {
        return payment.getId() + "T" + Long.toString(Instant.now().getEpochSecond(), 36);
    }

    /** The payment should end with the order's reservation (placed + pending timeout), within PayWay's limits. */
    private long lifetimeMinutes(Order order) {
        int timeoutMinutes = settingsService.get().getOrders().getPendingTimeoutMinutes();
        LocalDateTime orderExpires = order.getOrderDate().plusMinutes(timeoutMinutes);
        long left = Duration.between(LocalDateTime.now(ZoneId.systemDefault()), orderExpires).toMinutes();
        return Math.max(MIN_LIFETIME_MINUTES, Math.min(left, MAX_LIFETIME_MINUTES));
    }

    /** USD with cents (12.50); KHR has no minor unit (50000). */
    private static String formatAmount(BigDecimal amount, String currency) {
        int digits = "KHR".equals(currency) ? 0 : 2;
        return amount.setScale(digits, RoundingMode.HALF_UP).toPlainString();
    }

    private static String reqTime() {
        return LocalDateTime.now(ZoneOffset.UTC).format(REQ_TIME);
    }

    private String checkoutSignature(Long paymentId, String tranId) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(hmac("HmacSHA256", "checkout:" + paymentId + ":" + tranId));
    }

    private String hmacSha512Base64(String value) {
        return Base64.getEncoder().encodeToString(hmac("HmacSHA512", value));
    }

    private byte[] hmac(String algorithm, String value) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(publicKey.getBytes(StandardCharsets.UTF_8), algorithm));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign the ABA PayWay request", e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
