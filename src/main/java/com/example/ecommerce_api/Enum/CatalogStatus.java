package com.example.ecommerce_api.Enum;

/**
 * Products and variants are archived instead of deleted: orders, returns and stock history keep
 * pointing at them, but archived items are hidden from the shop and can't be bought.
 */
public enum CatalogStatus {
    ACTIVE,
    ARCHIVED
}
