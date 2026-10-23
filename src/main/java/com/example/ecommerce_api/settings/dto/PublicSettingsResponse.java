package com.example.ecommerce_api.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** What the storefront may see without logging in; never add order or security settings here. */
@Getter
@AllArgsConstructor
public class PublicSettingsResponse {

    private final String storeName;
    private final String description;
    private final String email;
    private final String phone;
    private final String currency;

    public static PublicSettingsResponse from(SettingsResponse s) {
        return new PublicSettingsResponse(s.getStore().getName(), s.getStore().getDescription(),
                s.getStore().getEmail(), s.getStore().getPhone(), s.getLocalization().getCurrency());
    }
}
