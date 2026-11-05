package com.example.ecommerce_api.payment.gateway;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.payment.entity.Payment;
import com.example.ecommerce_api.settings.service.SettingsService;
import com.example.ecommerce_api.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** The parts of the ABA PayWay gateway that don't call PayWay: checkout link, signed form, amounts. */
class AbaPayWayGatewayTest {

    private AbaPayWayGateway gateway;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        SettingsService settings = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settings.get().getOrders().getPendingTimeoutMinutes()).thenReturn(30);
        gateway = new AbaPayWayGateway("ec000002", "test-api-key", "https://checkout-sandbox.payway.com.kh/",
                "https://api.example.com/api/v1/payments/aba-payway/callback", "http://localhost:3000/", "cards", settings);

        User user = mock(User.class);
        when(user.getFirstName()).thenReturn("Dara");
        when(user.getLastName()).thenReturn("Sok");
        when(user.getEmail()).thenReturn("dara@example.com");
        when(user.getPhone()).thenReturn("012345678");
        order = mock(Order.class);
        when(order.getUser()).thenReturn(user);
        when(order.getOrderNumber()).thenReturn("ORD-1001");
        when(order.getOrderDate()).thenReturn(LocalDateTime.now());

        payment = mock(Payment.class);
        when(payment.getId()).thenReturn(42L);
        when(payment.getMethod()).thenReturn(PaymentMethod.ABA_PAYWAY);
        when(payment.getAmount()).thenReturn(new BigDecimal("12.5"));
        when(payment.getCurrency()).thenReturn("USD");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void initiateReturnsSignedCheckoutLinkAndShortTranId() {
        PaymentInitResult result = gateway.initiate(order, payment);

        assertThat(result.providerReference()).startsWith("42T").hasSizeLessThanOrEqualTo(20);
        assertThat(result.redirectUrl()).contains(AbaPayWayGateway.CHECKOUT_PATH + "42?sig=");

        when(payment.getProviderReference()).thenReturn(result.providerReference());
        String sig = UriComponentsBuilder.fromUriString(result.redirectUrl()).build().getQueryParams().getFirst("sig");
        assertThat(gateway.isValidCheckoutSignature(payment, sig)).isTrue();
        assertThat(gateway.isValidCheckoutSignature(payment, sig + "x")).isFalse();
        assertThat(gateway.isValidCheckoutSignature(payment, null)).isFalse();
    }

    @Test
    void purchaseFormIsSignedAndPointsBackToTheStorefront() {
        when(payment.getProviderReference()).thenReturn("42Tabc");

        Map<String, String> fields = gateway.purchaseFields(order, payment);

        assertThat(fields)
                .containsEntry("merchant_id", "ec000002")
                .containsEntry("tran_id", "42Tabc")
                .containsEntry("amount", "12.50")
                .containsEntry("currency", "USD")
                .containsEntry("continue_success_url", "http://localhost:3000/checkout/success?order=ORD-1001")
                .containsEntry("cancel_url", "http://localhost:3000/checkout/cancel?order=ORD-1001")
                .containsEntry("payment_option", "cards")
                .containsEntry("payment_gate", "0")
                .containsKeys("req_time", "return_url", "lifetime", "hash");
        assertThat(fields.values()).doesNotContain("test-api-key");
    }

    @Test
    void khrAmountsHaveNoDecimals() {
        when(payment.getProviderReference()).thenReturn("42Tabc");
        when(payment.getCurrency()).thenReturn("KHR");
        when(payment.getAmount()).thenReturn(new BigDecimal("50000.00"));

        assertThat(gateway.purchaseFields(order, payment)).containsEntry("amount", "50000");
    }

    @Test
    void otherCurrenciesAreRejected() {
        when(payment.getCurrency()).thenReturn("EUR");

        assertThatThrownBy(() -> gateway.initiate(order, payment)).isInstanceOf(BusinessException.class);
    }

    @Test
    void checkoutPagePostsTheSignedFormToPayWay() {
        when(payment.getProviderReference()).thenReturn("42Tabc");

        assertThat(gateway.checkoutPage(order, payment))
                .contains("action=\"https://checkout-sandbox.payway.com.kh/api/payment-gateway/v1/payments/purchase\"")
                .contains("name=\"payment_gate\" value=\"0\"")
                .contains("name=\"hash\"")
                .doesNotContain("test-api-key");
    }

    @Test
    void refundsAreManual() {
        assertThat(gateway.refund(payment, BigDecimal.ONE).manual()).isTrue();
    }
}
