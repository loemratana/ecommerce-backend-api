package com.example.ecommerce_api.returns.entity;

import com.example.ecommerce_api.Enum.ReturnItemCondition;
import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "return_items",
        indexes = @Index(name = "idx_return_item_order_item", columnList = "order_item_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity;

    /** Set when the warehouse receives the item. */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition", length = 20)
    private ReturnItemCondition condition;

    ReturnItem(ReturnRequest returnRequest, OrderItem orderItem, int quantity) {
        if (quantity < 1) {
            throw new BadRequestException("Return quantity must be at least 1");
        }
        this.returnRequest = returnRequest;
        this.orderItem = orderItem;
        this.quantity = quantity;
    }

    public void setCondition(ReturnItemCondition condition) {
        this.condition = condition;
    }
}
