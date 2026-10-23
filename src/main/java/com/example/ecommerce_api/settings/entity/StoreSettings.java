package com.example.ecommerce_api.settings.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The single row of store-wide settings (id is always {@link #SINGLETON_ID}). Read it through
 * SettingsService, which caches it, rather than from the repository.
 */
@Getter
@Setter
@Entity
@Table(name = "store_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Version
    private Long version;

    // ----- store ----------------------------------------------------------------------------

    @Column(name = "store_name", nullable = false, length = 150)
    private String storeName;

    @Column(name = "store_email", length = 150)
    private String storeEmail;

    @Column(name = "store_phone", length = 30)
    private String storePhone;

    @Column(name = "store_address")
    private String storeAddress;

    @Column(name = "store_description", length = 1000)
    private String storeDescription;

    // ----- orders ---------------------------------------------------------------------------

    /** e.g. 0.1000 = 10% tax on (subtotal - discount). */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;

    /** Unpaid orders are cancelled after this long, freeing their reserved stock. */
    @Column(name = "pending_timeout_minutes", nullable = false)
    private Integer pendingTimeoutMinutes;

    /** How long after delivery a customer may ask for a return. */
    @Column(name = "return_window_days", nullable = false)
    private Integer returnWindowDays;

    // ----- localization ---------------------------------------------------------------------

    /** ISO 4217 code. Only a label: amounts are never converted. */
    @Column(nullable = false, length = 3)
    private String currency;

    /** IANA zone id, e.g. Asia/Phnom_Penh; used when showing dates on invoices and exports. */
    @Column(nullable = false, length = 64)
    private String timezone;

    // ----- security -------------------------------------------------------------------------

    /** Lifetime of newly issued access tokens; tokens already issued keep their expiry. */
    @Column(name = "access_token_ttl_minutes", nullable = false)
    private Integer accessTokenTtlMinutes;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Email of the admin who saved last; null while the row still has its starting values. */
    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    /** A new, empty settings row; SettingsService fills in the starting values. */
    public static StoreSettings newSingleton() {
        return new StoreSettings();
    }
}
