package com.example.ecommerce_api.inventory.service;

import com.example.ecommerce_api.Enum.InventoryTransactionType;
import com.example.ecommerce_api.common.exception.InsufficientStockException;
import com.example.ecommerce_api.inventory.entity.Inventory;
import com.example.ecommerce_api.inventory.mapper.InventoryMapper;
import com.example.ecommerce_api.inventory.repository.InventoryRepository;
import com.example.ecommerce_api.inventory.repository.InventoryTransactionRepository;
import com.example.ecommerce_api.notification.service.NotificationService;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.product.repository.ProductVariantRepository;
import com.example.ecommerce_api.settings.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Low-stock alerts from stock changes, with mocked repositories and no database. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LowStockNotificationTest {

    private static final long VARIANT_ID = 7L;
    private static final int THRESHOLD = 10;

    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryTransactionRepository inventoryTransactionRepository;
    @Mock ProductVariantRepository productVariantRepository;
    @Mock InventoryMapper inventoryMapper;
    @Mock SettingsService settingsService;
    @Mock NotificationService notificationService;
    @InjectMocks InventoryServiceImpl inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setTitle("Basic Tee");
        ProductVariants variant = new ProductVariants();
        variant.setSku("TEE-M-BLK");
        variant.setProduct(product);
        inventory = new Inventory(variant, 12);

        when(inventoryRepository.lockByProductVariant_Id(VARIANT_ID)).thenReturn(Optional.of(inventory));
        when(settingsService.lowStockThreshold()).thenReturn(THRESHOLD);
    }

    @Test
    void reservingBelowTheThresholdNotifiesOnce() {
        inventoryService.reserve(VARIANT_ID, 3);   // 12 -> 9: crossed
        inventoryService.reserve(VARIANT_ID, 1);   // 9 -> 8: already low, no repeat

        verify(notificationService, times(1)).notifyLowStock("Basic Tee", "TEE-M-BLK", 9, THRESHOLD);
        verify(notificationService, times(1)).notifyLowStock(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void runningOutNotifiesAgainEvenWhenAlreadyLow() {
        inventoryService.reserve(VARIANT_ID, 3);   // 12 -> 9: low
        inventoryService.reserve(VARIANT_ID, 9);   // 9 -> 0: out of stock

        verify(notificationService).notifyLowStock("Basic Tee", "TEE-M-BLK", 9, THRESHOLD);
        verify(notificationService).notifyLowStock("Basic Tee", "TEE-M-BLK", 0, THRESHOLD);
    }

    @Test
    void reachingExactlyTheThresholdNotifies() {
        inventoryService.reserve(VARIANT_ID, 2);   // 12 -> 10: "at or below" counts

        verify(notificationService).notifyLowStock("Basic Tee", "TEE-M-BLK", 10, THRESHOLD);
    }

    @Test
    void stockStayingAboveTheThresholdDoesNotNotify() {
        Inventory plenty = new Inventory(inventory.getProductVariant(), 50);
        when(inventoryRepository.lockByProductVariant_Id(8L)).thenReturn(Optional.of(plenty));
        inventoryService.reserve(8L, 5);           // 50 -> 45

        verify(notificationService, never()).notifyLowStock(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void manualDamageAdjustmentNotifies() {
        inventoryService.adjust(VARIANT_ID, InventoryTransactionType.DAMAGE, -5, "water damage");   // 12 -> 7

        verify(notificationService).notifyLowStock("Basic Tee", "TEE-M-BLK", 7, THRESHOLD);
    }

    @Test
    void failedReservationDoesNotNotify() {
        assertThatThrownBy(() -> inventoryService.reserve(VARIANT_ID, 13))
                .isInstanceOf(InsufficientStockException.class);

        verify(notificationService, never()).notifyLowStock(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void crossingRules() {
        assertThat(InventoryServiceImpl.reachedLowStock(12, 9, 10)).isTrue();    // crossed the threshold
        assertThat(InventoryServiceImpl.reachedLowStock(11, 10, 10)).isTrue();   // landed exactly on it
        assertThat(InventoryServiceImpl.reachedLowStock(9, 8, 10)).isFalse();    // already low
        assertThat(InventoryServiceImpl.reachedLowStock(3, 0, 10)).isTrue();     // ran out
        assertThat(InventoryServiceImpl.reachedLowStock(0, 0, 10)).isFalse();    // was already out
        assertThat(InventoryServiceImpl.reachedLowStock(8, 20, 10)).isFalse();   // restocked
        assertThat(InventoryServiceImpl.reachedLowStock(5, 0, 0)).isTrue();      // threshold 0: only "out of stock"
        assertThat(InventoryServiceImpl.reachedLowStock(5, 1, 0)).isFalse();
    }
}
