package com.example.ecommerce_api.settings.dto;

import com.example.ecommerce_api.settings.entity.StoreSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable snapshot of the settings; SettingsService caches one and hands it to every caller. */
@Getter
@AllArgsConstructor
public class SettingsResponse {

    private final Store store;
    private final Orders orders;
    private final Localization localization;
    private final Security security;
    private final LocalDateTime updatedAt;
    private final String updatedBy;

    @Getter
    @AllArgsConstructor
    public static class Store {
        private final String name;
        private final String email;
        private final String phone;
        private final String address;
        private final String description;
    }

    @Getter
    @AllArgsConstructor
    public static class Orders {
        private final BigDecimal taxRate;
        private final BigDecimal shippingFee;
        private final int pendingTimeoutMinutes;
        private final int returnWindowDays;
    }

    @Getter
    @AllArgsConstructor
    public static class Localization {
        private final String currency;
        private final String timezone;
    }

    @Getter
    @AllArgsConstructor
    public static class Security {
        private final int accessTokenTtlMinutes;
    }

    public static SettingsResponse from(StoreSettings s) {
        return new SettingsResponse(
                new Store(s.getStoreName(), s.getStoreEmail(), s.getStorePhone(), s.getStoreAddress(), s.getStoreDescription()),
                new Orders(s.getTaxRate(), s.getShippingFee(), s.getPendingTimeoutMinutes(), s.getReturnWindowDays()),
                new Localization(s.getCurrency(), s.getTimezone()),
                new Security(s.getAccessTokenTtlMinutes()),
                s.getUpdatedAt(),
                s.getUpdatedBy());
    }
}
