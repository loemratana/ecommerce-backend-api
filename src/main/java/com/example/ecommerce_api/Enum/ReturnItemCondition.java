package com.example.ecommerce_api.Enum;

/** What the warehouse found when the returned item arrived. */
public enum ReturnItemCondition {
    /** Sellable: goes back into stock. */
    RESTOCK,
    /** Not sellable: refunded but written off, stock unchanged. */
    DAMAGED
}
