package com.example.ecommerce_api.payment.gateway;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Nothing happens online: the courier collects cash and the payment succeeds on delivery. */
@Component
public class CashOnDeliveryGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH_ON_DELIVERY;
    }

    @Override
    public PaymentInitResult initiate(Order order, Payment payment) {
        return PaymentInitResult.offline();
    }

    @Override
    public RefundResult refund(Payment payment, BigDecimal amount) {
        return new RefundResult(null, amount, true);
    }
}
