package com.example.ecommerce_api.returns.entity;

import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** A customer's request to send back some items of a delivered order. */
@Getter
@Entity
@Table(
        name = "return_requests",
        indexes = {
                @Index(name = "idx_return_order", columnList = "order_id"),
                @Index(name = "idx_return_user", columnList = "user_id"),
                @Index(name = "idx_return_status", columnList = "status")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_return_number", columnNames = "return_number")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public identifier, e.g. RET-20261019-3F9A1C7B. */
    @Column(name = "return_number", nullable = false, updatable = false, length = 32)
    private String returnNumber;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(nullable = false, length = 1000)
    private String reason;

    /** Shown to the customer, e.g. why it was rejected. */
    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    /** Amount refunded when the items were received; null before that. */
    @Column(name = "refund_amount", precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    public ReturnRequest(Order order, User user, String reason) {
        this.order = order;
        this.user = user;
        this.reason = reason;
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        this.returnNumber = "RET-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + random;
    }

    public List<ReturnItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItem orderItem, int quantity) {
        if (orderItem.getOrder() != order) {
            throw new BusinessException("Item does not belong to order " + order.getOrderNumber());
        }
        items.add(new ReturnItem(this, orderItem, quantity));
    }

    /** Value of the returned goods at the price the customer paid per unit (before order-level discounts). */
    public BigDecimal getGoodsValue() {
        return items.stream()
                .map(item -> item.getOrderItem().getPriceAfterDiscount()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ----- status transitions -----------------------------------------------------------------

    public void approve(String note) {
        transitionTo(ReturnStatus.APPROVED);
        this.adminNote = note;
    }

    public void reject(String note) {
        transitionTo(ReturnStatus.REJECTED);
        this.adminNote = note;
    }

    public void cancel() {
        transitionTo(ReturnStatus.CANCELLED);
    }

    /** Stock and the order's refund total are updated by the caller. */
    public void markReceived(BigDecimal refundAmount, String note) {
        transitionTo(ReturnStatus.RECEIVED);
        this.refundAmount = refundAmount;
        this.receivedAt = LocalDateTime.now();
        if (note != null && !note.isBlank()) {
            this.adminNote = note;
        }
    }

    private void transitionTo(ReturnStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessException("Cannot change return " + returnNumber + " from " + status + " to " + next);
        }
        status = next;
    }
}
