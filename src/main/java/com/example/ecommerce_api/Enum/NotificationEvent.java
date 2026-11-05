package com.example.ecommerce_api.Enum;

/** Something that happened in the store that admins can subscribe a notification destination to. */
public enum NotificationEvent {
    /** A customer placed an order (status PENDING, before payment). */
    NEW_ORDER,
    /** A variant's available stock dropped to the low-stock threshold, or ran out. */
    LOW_STOCK,
    /** An admin created a product. */
    NEW_PRODUCT
}
