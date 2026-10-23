package com.example.ecommerce_api.settings.service;

import com.example.ecommerce_api.common.exception.BadRequestException;
import com.example.ecommerce_api.settings.dto.*;
import com.example.ecommerce_api.settings.entity.StoreSettings;
import com.example.ecommerce_api.settings.repository.StoreSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Currency;
import java.util.function.Consumer;

/**
 * Store-wide settings edited from the dashboard. Order placement, the pending-order job, returns,
 * invoices, exports and token issuing all read them, so the row is cached and reloaded every
 * {@link #CACHE_TTL} (also picks up changes saved by other app instances) or right after a save.
 *
 * The row is created on first use from the app.* values in application.yaml.
 */
@Service
@Slf4j
public class SettingsService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final StoreSettingsRepository repository;
    private final TransactionTemplate newTransaction;

    // ----- starting values, only used to create the row ---------------------------------------
    @Value("${app.invoice.company-name:E-Commerce Store}")
    private String defaultStoreName;
    @Value("${app.invoice.company-address:}")
    private String defaultStoreAddress;
    @Value("${app.invoice.company-email:}")
    private String defaultStoreEmail;
    @Value("${app.order.tax-rate:0.00}")
    private BigDecimal defaultTaxRate;
    @Value("${app.order.shipping-fee:0.00}")
    private BigDecimal defaultShippingFee;
    @Value("${app.order.pending-timeout:PT30M}")
    private Duration defaultPendingTimeout;
    @Value("${app.order.return-window:P30D}")
    private Duration defaultReturnWindow;
    @Value("${app.invoice.currency:USD}")
    private String defaultCurrency;
    @Value("${app.localization.timezone:Asia/Phnom_Penh}")
    private String defaultTimezone;
    @Value("${jwt.access-token-ttl-seconds:9000}")
    private long defaultAccessTokenTtlSeconds;

    private record Cached(SettingsResponse value, Instant loadedAt) {
    }

    private volatile Cached cached;

    public SettingsService(StoreSettingsRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        // Creating the row must not run inside the caller's transaction, which may be read-only.
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ----- reading ----------------------------------------------------------------------------

    public SettingsResponse get() {
        Cached current = cached;
        if (current == null || current.loadedAt().plus(CACHE_TTL).isBefore(Instant.now())) {
            current = new Cached(SettingsResponse.from(loadOrCreate()), Instant.now());
            cached = current;
        }
        return current.value();
    }

    public PublicSettingsResponse getPublic() {
        return PublicSettingsResponse.from(get());
    }

    public ZoneId zone() {
        return ZoneId.of(get().getLocalization().getTimezone());
    }

    /** Timestamps are stored in the server's zone; this is the same moment on the store's clock. */
    public LocalDateTime toStoreTime(LocalDateTime serverTime) {
        return serverTime == null ? null
                : serverTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone()).toLocalDateTime();
    }

    /** The reverse of {@link #toStoreTime}, e.g. for turning a date the admin picked into a query bound. */
    public LocalDateTime toServerTime(LocalDateTime storeTime) {
        return storeTime.atZone(zone()).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    // ----- updating (admin) -------------------------------------------------------------------

    @Transactional
    public SettingsResponse updateStore(StoreSettingsRequest request) {
        return update(s -> {
            s.setStoreName(request.getName().trim());
            s.setStoreEmail(blankToNull(request.getEmail()));
            s.setStorePhone(blankToNull(request.getPhone()));
            s.setStoreAddress(blankToNull(request.getAddress()));
            s.setStoreDescription(blankToNull(request.getDescription()));
        });
    }

    /** New values apply to orders placed from now on; existing orders keep the tax and shipping they were charged. */
    @Transactional
    public SettingsResponse updateOrders(OrderSettingsRequest request) {
        return update(s -> {
            s.setTaxRate(request.getTaxRate().setScale(4, RoundingMode.HALF_UP));
            s.setShippingFee(request.getShippingFee().setScale(2, RoundingMode.HALF_UP));
            s.setPendingTimeoutMinutes(request.getPendingTimeoutMinutes());
            s.setReturnWindowDays(request.getReturnWindowDays());
        });
    }

    @Transactional
    public SettingsResponse updateLocalization(LocalizationSettingsRequest request) {
        String currency = request.getCurrency();
        String timezone = request.getTimezone().trim();
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown currency code: " + currency);
        }
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new BadRequestException("Unknown timezone: " + timezone);
        }
        return update(s -> {
            s.setCurrency(currency);
            s.setTimezone(timezone);
        });
    }

    /** Only affects tokens issued from now on; people already logged in keep their current expiry. */
    @Transactional
    public SettingsResponse updateSecurity(SecuritySettingsRequest request) {
        return update(s -> s.setAccessTokenTtlMinutes(request.getAccessTokenTtlMinutes()));
    }

    private SettingsResponse update(Consumer<StoreSettings> change) {
        StoreSettings settings = loadOrCreate();
        change.accept(settings);
        settings.setUpdatedBy(currentUserEmail());
        // Flush so @UpdateTimestamp is set before building the response; a concurrent save by
        // another admin fails here on @Version and becomes a 409.
        repository.saveAndFlush(settings);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cached = null;
            }
        });
        return SettingsResponse.from(settings);
    }

    // ----- loading ----------------------------------------------------------------------------

    private StoreSettings loadOrCreate() {
        return repository.findById(StoreSettings.SINGLETON_ID).orElseGet(() -> {
            try {
                newTransaction.executeWithoutResult(status -> repository.save(startingValues()));
                log.info("Created store settings from application.yaml values");
            } catch (DataIntegrityViolationException e) {
                // Another request or instance created it first.
            }
            return repository.findById(StoreSettings.SINGLETON_ID)
                    .orElseThrow(() -> new IllegalStateException("Store settings row could not be created"));
        });
    }

    private StoreSettings startingValues() {
        StoreSettings s = StoreSettings.newSingleton();
        s.setStoreName(defaultStoreName);
        s.setStoreEmail(blankToNull(defaultStoreEmail));
        s.setStoreAddress(blankToNull(defaultStoreAddress));
        s.setTaxRate(defaultTaxRate.setScale(4, RoundingMode.HALF_UP));
        s.setShippingFee(defaultShippingFee.setScale(2, RoundingMode.HALF_UP));
        s.setPendingTimeoutMinutes((int) defaultPendingTimeout.toMinutes());
        s.setReturnWindowDays((int) defaultReturnWindow.toDays());
        s.setCurrency(defaultCurrency);
        s.setTimezone(defaultTimezone);
        s.setAccessTokenTtlMinutes((int) Math.max(1, defaultAccessTokenTtlSeconds / 60));
        return s;
    }

    private static String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
