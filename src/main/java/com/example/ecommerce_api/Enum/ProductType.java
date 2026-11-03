package com.example.ecommerce_api.Enum;

import java.util.List;

/**
 * Who a product is for; the shop uses it for the Men / Women sections and filters.
 * UNISEX products show up under both Men and Women.
 */
public enum ProductType {
    MEN,
    WOMEN,
    UNISEX;

    /** Types to show for this section: MEN and WOMEN include UNISEX, UNISEX is only itself. */
    public List<ProductType> withUnisex() {
        return this == UNISEX ? List.of(UNISEX) : List.of(this, UNISEX);
    }
}
