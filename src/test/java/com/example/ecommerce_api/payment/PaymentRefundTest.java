package com.example.ecommerce_api.payment;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.payment.entity.Payment;
import com.example.ecommerce_api.payment.gateway.*;
import com.example.ecommerce_api.payment.repository.PaymentRepository;
import com.example.ecommerce_api.payment.repository.ProcessedPaymentEventRepository;
import com.example.ecommerce_api.payment.service.PaymentServiceImpl;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Automatic refunds (phase 3), with a fake gateway instead of Stripe and no database. */
class PaymentRefundTest {

    /** Records refunds instead of calling a provider; can be told to refuse. */
    static class FakeGateway implements PaymentGateway {
        final PaymentMethod method;
        final boolean manual;
        final List<BigDecimal> refunds = new ArrayList<>();
        boolean refuse;

        FakeGateway(PaymentMethod method, boolean manual) {
            this.method = method;
            this.manual = manual;
        }

        @Override public PaymentMethod method() { return method; }
        @Override public PaymentInitResult initiate(Order order, Payment payment) { return PaymentInitResult.offline(); }

        @Override
        public RefundResult refund(Payment payment, BigDecimal amount) {
            if (refuse) throw new BusinessException("The refund could not be processed by Stripe: card closed");
            refunds.add(amount);
            return new RefundResult(manual ? null : "re_" + refunds.size(), amount, manual);
        }
    }

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ProcessedPaymentEventRepository eventRepository = mock(ProcessedPaymentEventRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final FakeGateway stripe = new FakeGateway(PaymentMethod.STRIPE, false);
    private final FakeGateway cash = new FakeGateway(PaymentMethod.CASH_ON_DELIVERY, true);
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        PaymentGatewayResolver resolver = new PaymentGatewayResolver(List.of(stripe, cash));
        // Mapper, security and settings aren't used by the refund paths.
        service = new PaymentServiceImpl(paymentRepository, eventRepository, orderRepository, resolver, null, null, null);
    }

    /** One 60.00 item, so the order total is 60.00. */
    private static Order pendingOrder() {
        Product product = new Product();
        product.setTitle("Tee");
        ProductVariants variant = new ProductVariants();
        variant.setProduct(product);
        variant.setSku("TEE-1");
        variant.setPrice(new BigDecimal("60.00"));
        Order order = new Order(new User(), null);
        order.addItem(new OrderItem(variant, 1));
        return order;
    }

    private static Payment capturedPayment(Order order, PaymentMethod method) {
        Payment payment = new Payment(order, method, order.getTotalOrderPrice(), "USD");
        payment.attachProvider("cs_test_1", null);
        payment.markSucceeded();
        return payment;
    }

    private void givenPayments(Payment... payments) {
        when(paymentRepository.findByOrderIdOrderByCreatedAtDesc(any())).thenReturn(List.of(payments));
    }

    @Test
    void cancellingAPaidOrderRefundsEverythingThroughTheGateway() {
        Order order = pendingOrder();
        order.markPaid();
        Payment payment = capturedPayment(order, PaymentMethod.STRIPE);
        givenPayments(payment);

        service.onOrderCancelled(order);

        assertThat(stripe.refunds).containsExactly(new BigDecimal("60.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void cancellingAnUnpaidOrderRefundsNothing() {
        Order order = pendingOrder();
        Payment open = new Payment(order, PaymentMethod.STRIPE, order.getTotalOrderPrice(), "USD");
        givenPayments(open);

        service.onOrderCancelled(order);

        assertThat(stripe.refunds).isEmpty();
        assertThat(open.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void partialRefundsAddUpAndNeverExceedWhatWasPaid() {
        Order order = pendingOrder();
        Payment payment = capturedPayment(order, PaymentMethod.STRIPE);
        givenPayments(payment);

        assertThat(service.refund(order, new BigDecimal("25.00"), "Return R1")).isEqualByComparingTo("25.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);

        // Asking for more than is left only refunds what's left
        assertThat(service.refund(order, new BigDecimal("50.00"), "Return R2")).isEqualByComparingTo("35.00");
        assertThat(stripe.refunds).containsExactly(new BigDecimal("25.00"), new BigDecimal("35.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void withoutACapturedPaymentNothingGoesThroughAGateway() {
        Order order = pendingOrder();
        order.markPaid();   // e.g. marked paid by an admin, no payment record
        givenPayments();

        assertThat(service.refund(order, new BigDecimal("20.00"), "Return R1")).isEqualByComparingTo("0");
        assertThat(stripe.refunds).isEmpty();
    }

    @Test
    void cashOnDeliveryRefundsAreRecordedAsManual() {
        Order order = pendingOrder();
        Payment payment = capturedPayment(order, PaymentMethod.CASH_ON_DELIVERY);
        givenPayments(payment);

        service.refund(order, new BigDecimal("60.00"), "Return R1");

        assertThat(cash.refunds).containsExactly(new BigDecimal("60.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void aRefusedRefundThrowsAndRecordsNothing() {
        Order order = pendingOrder();
        order.markPaid();
        Payment payment = capturedPayment(order, PaymentMethod.STRIPE);
        givenPayments(payment);
        stripe.refuse = true;

        assertThatThrownBy(() -> service.onOrderCancelled(order))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("refund could not be processed");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("0");
    }

    @Test
    void aPaymentForAnAlreadyCancelledOrderIsRefundedRightAway() {
        Order order = pendingOrder();
        order.cancel();   // expired while the customer was on Stripe's page
        Payment payment = new Payment(order, PaymentMethod.STRIPE, order.getTotalOrderPrice(), "USD");
        payment.attachProvider("cs_test_1", "https://checkout.stripe.com/c/pay/cs_test_1");
        givenEventFor(order, payment);

        service.handleGatewayEvent(PaymentMethod.STRIPE, new GatewayEvent(
                "evt_1", GatewayEvent.Type.PAYMENT_SUCCEEDED, "cs_test_1", new BigDecimal("60.00"), null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stripe.refunds).containsExactly(new BigDecimal("60.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    // ----- charge.refunded: refunds reported by the gateway ---------------------------------------

    /** Wires the lookups handleGatewayEvent does for the payment with provider reference cs_test_1. */
    private void givenEventFor(Order order, Payment payment) {
        when(eventRepository.existsByMethodAndEventId(any(), any())).thenReturn(false);
        when(paymentRepository.findOrderNumberByProviderReference(PaymentMethod.STRIPE, "cs_test_1"))
                .thenReturn(Optional.of(order.getOrderNumber()));
        when(orderRepository.lockByOrderNumber(eq(order.getOrderNumber()))).thenReturn(Optional.of(order));
        when(paymentRepository.lockByProviderReference(PaymentMethod.STRIPE, "cs_test_1")).thenReturn(Optional.of(payment));
    }

    private void refundedAtStripe(String eventId, String totalRefunded) {
        service.handleGatewayEvent(PaymentMethod.STRIPE, new GatewayEvent(
                eventId, GatewayEvent.Type.REFUNDED, "cs_test_1", new BigDecimal(totalRefunded), null));
    }

    private static Order deliveredOrder() {
        Order order = pendingOrder();
        order.markPaid();
        order.startProcessing();
        order.markShipped();
        order.markDelivered();
        return order;
    }

    @Test
    void aDashboardRefundOnADeliveredOrderIsRecordedOnPaymentAndOrder() {
        Order order = deliveredOrder();
        Payment payment = capturedPayment(order, PaymentMethod.STRIPE);
        givenEventFor(order, payment);

        refundedAtStripe("evt_r1", "15.00");

        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("15.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(order.getRefundedAmount()).isEqualByComparingTo("15.00");
        assertThat(stripe.refunds).isEmpty();   // reported by Stripe, not requested by us
    }

    @Test
    void theEchoOfOurOwnRefundChangesNothing() {
        Order order = deliveredOrder();
        Payment payment = capturedPayment(order, PaymentMethod.STRIPE);
        givenPayments(payment);
        givenEventFor(order, payment);

        service.refund(order, new BigDecimal("20.00"), "Return R1");   // we refund 20.00...
        refundedAtStripe("evt_r1", "20.00");                            // ...Stripe reports a total of 20.00

        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("20.00");
        assertThat(stripe.refunds).containsExactly(new BigDecimal("20.00"));
    }

    @Test
    void runningTotalsOnlyAddTheNewPart() {
        Order order = deliveredOrder();
        Payment payment = capturedPayment(order, PaymentMethod.STRIPE);
        givenEventFor(order, payment);

        refundedAtStripe("evt_r1", "10.00");
        refundedAtStripe("evt_r2", "25.00");   // a second dashboard refund of 15.00

        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("25.00");
        assertThat(order.getRefundedAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void aFullDashboardRefundBeforeShippingRefundsThePaymentButKeepsTheOrder() {
        Order order = pendingOrder();
        order.markPaid();
        Payment payment = capturedPayment(order, PaymentMethod.STRIPE);
        givenEventFor(order, payment);

        refundedAtStripe("evt_r1", "60.00");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        // Not cancelled automatically (an admin decides); order refunds only count once delivered.
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getRefundedAmount()).isEqualByComparingTo("0");
    }
}
