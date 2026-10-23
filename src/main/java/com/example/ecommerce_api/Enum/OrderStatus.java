package com.example.ecommerce_api.Enum;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    PENDING("Order received, waiting for payment"),
    PAID("Payment received, waiting to be packed"),
    PROCESSING("Order is being packed"),
    SHIPPED("Order is on the way"),
    DELIVERED("Order delivered successfully"),
    CANCELLED("Order was cancelled");

    private String description;


    OrderStatus(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public  boolean isFinalStatus()
    {
        return this == DELIVERED || this == CANCELLED;
    }

    /** Statuses whose order total counts as revenue: paid and not cancelled. */
    public static Set<OrderStatus> revenueStatuses()
    {
        return EnumSet.of(PAID, PROCESSING, SHIPPED, DELIVERED);
    }

    /**
     * PENDING -> PAID -> PROCESSING -> SHIPPED -> DELIVERED.
     * An order can be cancelled until it has shipped.
     */
    public boolean canTransitionTo(OrderStatus next)
    {
        return switch (this) {
            case PENDING -> next == PAID || next == CANCELLED;
            case PAID -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
