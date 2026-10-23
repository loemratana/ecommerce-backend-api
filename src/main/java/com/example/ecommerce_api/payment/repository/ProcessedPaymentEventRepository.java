package com.example.ecommerce_api.payment.repository;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.payment.entity.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, Long> {

    boolean existsByMethodAndEventId(PaymentMethod method, String eventId);
}
