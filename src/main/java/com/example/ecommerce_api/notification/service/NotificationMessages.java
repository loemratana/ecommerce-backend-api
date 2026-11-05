package com.example.ecommerce_api.notification.service;

import com.example.ecommerce_api.notification.strategy.NotificationMessage;
import com.example.ecommerce_api.order.dto.OrderItemResponse;
import com.example.ecommerce_api.order.dto.OrderResponse;
import com.example.ecommerce_api.order.dto.ShippingAddressResponse;
import com.example.ecommerce_api.product.dto.response.ProductResponse;
import com.example.ecommerce_api.product.dto.response.ProductVariantResponse;
import com.example.ecommerce_api.settings.dto.SettingsResponse;
import com.example.ecommerce_api.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wording of every notification, kept out of the order, inventory and product services: a plain
 * text version (Telegram, email text part) and the Thymeleaf template + variables for the HTML
 * email (resources/templates/email/).
 */
@Component
@RequiredArgsConstructor
public class NotificationMessages {

    // Longer orders are summarized so the Telegram message stays readable
    static final int MAX_ORDER_LINES = 10;

    private final SettingsService settingsService;

    public NotificationMessage newOrder(OrderResponse order) {
        StringBuilder body = new StringBuilder();
        if (order.getCustomer() != null) {
            body.append("Customer: ").append(order.getCustomer().getName());
            if (order.getCustomer().getEmail() != null) {
                body.append(" (").append(order.getCustomer().getEmail()).append(')');
            }
            body.append('\n');
        }

        List<OrderItemResponse> items = order.getItems() == null ? List.of() : order.getItems();
        body.append("Items:\n");
        items.stream().limit(MAX_ORDER_LINES).forEach(item -> body
                .append("• ").append(item.getQuantity()).append(" × ").append(item.getProductName())
                .append(" (").append(item.getSku()).append(")\n"));
        if (items.size() > MAX_ORDER_LINES) {
            body.append("• … and ").append(items.size() - MAX_ORDER_LINES).append(" more\n");
        }

        if (order.getDiscountAmount() != null && order.getDiscountAmount().signum() > 0) {
            body.append("Discount: -").append(money(order.getDiscountAmount())).append('\n');
        }
        body.append("Total: ").append(money(order.getTotalOrderPrice())).append('\n');
        body.append("Status: ").append(order.getStatus());

        Map<String, Object> model = storeModel();
        model.put("order", order);
        model.put("items", items);
        return new NotificationMessage("🛒 New order " + order.getOrderNumber(), body.toString(),
                "email/new-order", model);
    }

    /** For the customer who placed the order: what they bought, what it costs and where it goes. */
    public NotificationMessage orderConfirmation(OrderResponse order) {
        String storeName = settingsService.get().getStore().getName();
        List<OrderItemResponse> items = order.getItems() == null ? List.of() : order.getItems();
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;

        StringBuilder body = new StringBuilder();
        body.append(StringUtils.hasText(customerName) ? "Hi " + customerName.trim() + ",\n\n" : "Hello,\n\n");
        body.append("Thank you for your order. We've received it and will let you know when it ships.\n\n");
        body.append("Order number: ").append(order.getOrderNumber()).append("\n\n");
        items.forEach(item -> body
                .append("• ").append(item.getQuantity()).append(" × ").append(item.getProductName())
                .append(" — ").append(money(item.getLineTotal())).append('\n'));
        body.append('\n');
        body.append("Subtotal: ").append(money(order.getSubtotal())).append('\n');
        if (order.getDiscountAmount() != null && order.getDiscountAmount().signum() > 0) {
            body.append("Discount: -").append(money(order.getDiscountAmount())).append('\n');
        }
        if (order.getTaxPrice() != null && order.getTaxPrice().signum() > 0) {
            body.append("Tax: ").append(money(order.getTaxPrice())).append('\n');
        }
        body.append("Shipping: ").append(money(order.getShippingPrice())).append('\n');
        body.append("Total: ").append(money(order.getTotalOrderPrice())).append('\n');
        if (order.getShippingAddress() != null) {
            body.append("\nShipping to:\n").append(addressText(order.getShippingAddress())).append('\n');
        }

        Map<String, Object> model = storeModel();
        model.put("customerFacing", true);
        model.put("order", order);
        model.put("items", items);
        model.put("customerName", customerName);
        model.put("address", order.getShippingAddress());
        model.put("cityLine", order.getShippingAddress() != null ? cityLine(order.getShippingAddress()) : null);
        model.put("orderDate", settingsService.toStoreTime(order.getOrderDate()));
        return new NotificationMessage("Your " + storeName + " order " + order.getOrderNumber() + " is received",
                body.toString(), "email/order-confirmation", model);
    }

    public NotificationMessage lowStock(String productTitle, String sku, int available, int threshold) {
        boolean outOfStock = available <= 0;
        String title = outOfStock
                ? "⛔ Out of stock: " + productTitle
                : "⚠️ Low stock: " + productTitle;
        String body = "SKU: " + sku + "\n"
                + "Available: " + Math.max(available, 0) + " (alert threshold " + threshold + ")";

        Map<String, Object> model = storeModel();
        model.put("productTitle", productTitle);
        model.put("sku", sku);
        model.put("available", Math.max(available, 0));
        model.put("threshold", threshold);
        model.put("outOfStock", outOfStock);
        return new NotificationMessage(title, body, "email/low-stock", model);
    }

    public NotificationMessage newProduct(ProductResponse product) {
        String brand = product.getBrand() != null ? product.getBrand().getBrandName() : null;
        String category = product.getSubCategory() != null ? product.getSubCategory().getSubCategoryName() : null;
        List<ProductVariantResponse> variants = product.getVariants() == null ? List.of() : product.getVariants();
        BigDecimal priceFrom = variants.stream()
                .map(v -> v.getPriceAfterDiscount() != null ? v.getPriceAfterDiscount() : v.getPrice())
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        int stock = variants.stream()
                .map(ProductVariantResponse::getAvailableQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        StringBuilder body = new StringBuilder();
        if (brand != null) {
            body.append("Brand: ").append(brand).append('\n');
        }
        if (category != null) {
            body.append("Category: ").append(category).append('\n');
        }
        body.append("Variants: ").append(variants.size()).append('\n');
        if (priceFrom != null) {
            body.append("Price from: ").append(money(priceFrom)).append('\n');
        }
        body.append("Stock: ").append(stock).append('\n');
        body.append("Slug: ").append(product.getSlug());

        Map<String, Object> model = storeModel();
        model.put("product", product);
        model.put("brand", brand);
        model.put("category", category);
        model.put("variants", variants);
        model.put("priceFrom", priceFrom);
        model.put("stock", stock);
        return new NotificationMessage("🆕 New product: " + product.getTitle(), body.toString(),
                "email/new-product", model);
    }

    /** Variables every email template uses (layout header and footer, money formatting). */
    private Map<String, Object> storeModel() {
        SettingsResponse settings = settingsService.get();
        SettingsResponse.Store store = settings.getStore();
        Map<String, Object> model = new HashMap<>();
        model.put("storeName", store != null ? store.getName() : null);
        model.put("storeEmail", store != null ? store.getEmail() : null);
        model.put("storePhone", store != null ? store.getPhone() : null);
        model.put("currency", settings.getLocalization() != null ? settings.getLocalization().getCurrency() : null);
        return model;
    }

    private static String addressText(ShippingAddressResponse a) {
        return Stream.of(a.getRecipientName(), a.getPhone(), a.getStreet(), cityLine(a), a.getCountry())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    /** "Phnom Penh 12000": city, state and zip code, skipping the blank ones. */
    private static String cityLine(ShippingAddressResponse a) {
        return Stream.of(a.getCity(), a.getState(), a.getZipCode())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString() + " "
                + settingsService.get().getLocalization().getCurrency();
    }
}
