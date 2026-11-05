package com.example.ecommerce_api.product;

import com.example.ecommerce_api.cart.entity.Cart;
import com.example.ecommerce_api.cart.entity.CartItem;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** The discount is set on the product only; cart and order use the same unit price. */
class ProductDiscountPricingTest {

    private static ProductVariants variant(String price, String productDiscount) {
        Product product = new Product();
        product.setTitle("Shirt");
        product.setDiscountPercent(productDiscount == null ? null : new BigDecimal(productDiscount));
        ProductVariants variant = new ProductVariants();
        variant.setSku("SKU-1");
        variant.setPrice(new BigDecimal(price));
        variant.setProduct(product);
        return variant;
    }

    @Test
    void noProductDiscountMeansFullPrice() {
        ProductVariants v = variant("19.99", null);
        assertThat(v.getEffectiveDiscountPercent()).isNull();
        assertThat(v.getPriceAfterDiscount()).isEqualByComparingTo("19.99");
        assertThat(v.getDiscountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void productDiscountIsAppliedAndRoundedToCents() {
        ProductVariants v = variant("19.99", "15");
        // 19.99 * 15% = 2.9985 -> 3.00 off
        assertThat(v.getDiscountAmount()).isEqualByComparingTo("3.00");
        assertThat(v.getPriceAfterDiscount()).isEqualByComparingTo("16.99");
        assertThat(v.getPriceAfterDiscount().add(v.getDiscountAmount())).isEqualByComparingTo(v.getPrice());
    }

    @Test
    void zeroDiscountCountsAsNone() {
        assertThat(variant("10.00", "0").getEffectiveDiscountPercent()).isNull();
    }

    @Test
    void cartTotalsShowRegularAndDiscountedPrice_andMatchOrderLine() {
        ProductVariants v = variant("20.00", "25");
        CartItem item = new CartItem();
        item.setProductVariants(v);
        item.setQuantity(3);
        item.setPrice(v.getPriceAfterDiscount());
        Cart cart = new Cart();
        cart.addItem(item);
        cart.recalculateTotalPrice();

        assertThat(cart.getTotalCartPrice()).isEqualByComparingTo("60.00");           // 3 x 20.00
        assertThat(cart.getTotalPriceAfterDiscount()).isEqualByComparingTo("45.00");  // 3 x 15.00

        OrderItem line = new OrderItem(v, 3);
        assertThat(line.getPrice()).isEqualByComparingTo("20.00");
        assertThat(line.getDiscountPercent()).isEqualByComparingTo("25");
        assertThat(line.getLineTotal()).isEqualByComparingTo(cart.getTotalPriceAfterDiscount());
    }
}
