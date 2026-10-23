package com.example.ecommerce_api.payment.repository;

import com.example.ecommerce_api.Enum.PaymentMethod;
import com.example.ecommerce_api.Enum.PaymentStatus;
import com.example.ecommerce_api.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    /** Unlocked lookup, so event handling can lock the order before the payment (see PaymentServiceImpl). */
    @Query("SELECT p.order.orderNumber FROM Payment p WHERE p.method = :method AND p.providerReference = :reference")
    Optional<String> findOrderNumberByProviderReference(@Param("method") PaymentMethod method,
                                                        @Param("reference") String providerReference);

    /** Used by gateway events; locked so two deliveries of related events can't both apply. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.method = :method AND p.providerReference = :reference")
    Optional<Payment> lockByProviderReference(@Param("method") PaymentMethod method,
                                              @Param("reference") String providerReference);

    /** Admin list; null status or method means any. */
    @EntityGraph(attributePaths = {"order", "order.user"})
    @Query("SELECT p FROM Payment p WHERE (:status IS NULL OR p.status = :status) AND (:method IS NULL OR p.method = :method)")
    Page<Payment> search(@Param("status") PaymentStatus status,
                         @Param("method") PaymentMethod method,
                         Pageable pageable);
}
