package com.example.ecommerce_api.payment.gateway;

import java.math.BigDecimal;

/** manual = the money has to be handed back outside the system (e.g. cash). */
public record RefundResult(String providerRefundId, BigDecimal amount, boolean manual) {
}
