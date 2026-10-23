package com.example.ecommerce_api.payment.gateway;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Picks the strategy for a payment method; Spring injects every PaymentGateway bean. */
@Component
public class PaymentGatewayResolver {

    private final Map<PaymentMethod, PaymentGateway> gateways;

    public PaymentGatewayResolver(List<PaymentGateway> gateways) {
        Map<PaymentMethod, PaymentGateway> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentGateway gateway : gateways) {
            PaymentGateway previous = byMethod.put(gateway.method(), gateway);
            if (previous != null) {
                throw new IllegalStateException("Two payment gateways for " + gateway.method() + ": "
                        + previous.getClass().getSimpleName() + " and " + gateway.getClass().getSimpleName());
            }
        }
        this.gateways = Collections.unmodifiableMap(byMethod);
    }

    public PaymentGateway get(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new BadRequestException("Payment method " + method + " is not available");
        }
        return gateway;
    }

    public Set<PaymentMethod> availableMethods() {
        return gateways.keySet();
    }
}
