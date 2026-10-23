package com.example.ecommerce_api.returns;

import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.common.exception.BusinessException;
import com.example.ecommerce_api.order.entity.Order;
import com.example.ecommerce_api.order.entity.OrderItem;
import com.example.ecommerce_api.product.entity.Product;
import com.example.ecommerce_api.product.entity.ProductVariants;
import com.example.ecommerce_api.returns.entity.ReturnRequest;
import com.example.ecommerce_api.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Refund maths and return status rules, without a database. */
class ReturnRulesTest {

    private static OrderItem line(String sku, String price, int quantity) {
        Product product = new Product();
        product.setTitle("Tee " + sku);
        ProductVariants variant = new ProductVariants();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setPrice(new BigDecimal(price));
        return new OrderItem(variant, quantity);
    }

    /** 2 x 50 + 1 x 20 = 120 subtotal, 12 tax, 5 shipping = 137 total. */
    private static Order deliveredOrder(OrderItem... items) {
        Order order = new Order(new User(), null);
        for (OrderItem item : items) order.addItem(item);
        order.setCharges(new BigDecimal("12.00"), new BigDecimal("5.00"));
        order.markPaid();
        order.startProcessing();
        order.markShipped();
        order.markDelivered();
        return order;
    }

    @Test
    void refundSharesTaxButNotShipping() {
        OrderItem shirts = line("A", "50.00", 2);
        Order order = deliveredOrder(shirts, line("B", "20.00", 1));

        assertThat(order.getTotalOrderPrice()).isEqualByComparingTo("137.00");
        assertThat(order.getDeliveredAt()).isNotNull();
        // one shirt = 50 of a 120 subtotal; the customer paid 132 for goods -> 50 * 132 / 120 = 55
        assertThat(order.refundFor(new BigDecimal("50.00"))).isEqualByComparingTo("55.00");
        // everything except shipping
        assertThat(order.getRefundableAmount()).isEqualByComparingTo("132.00");
    }

    @Test
    void refundsCanNeverExceedWhatWasPaidForGoods() {
        Order order = deliveredOrder(line("A", "50.00", 2), line("B", "20.00", 1));

        order.recordRefund(order.refundFor(new BigDecimal("100.00")));   // both shirts: 110
        assertThat(order.getRefundedAmount()).isEqualByComparingTo("110.00");
        assertThat(order.getRefundableAmount()).isEqualByComparingTo("22.00");
        // capped at what is left
        assertThat(order.refundFor(new BigDecimal("100.00"))).isEqualByComparingTo("22.00");
        assertThatThrownBy(() -> order.recordRefund(new BigDecimal("22.01")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void refundRequiresDeliveredOrder() {
        Order order = new Order(new User(), null);
        order.addItem(line("A", "10.00", 1));
        assertThatThrownBy(() -> order.recordRefund(BigDecimal.ONE)).isInstanceOf(BusinessException.class);
    }

    @Test
    void returnFollowsRequestedApprovedReceived() {
        OrderItem shirts = line("A", "50.00", 2);
        Order order = deliveredOrder(shirts);
        ReturnRequest request = new ReturnRequest(order, new User(), "Too small");
        request.addItem(shirts, 1);

        assertThat(request.getReturnNumber()).startsWith("RET-");
        assertThat(request.getGoodsValue()).isEqualByComparingTo("50.00");

        assertThatThrownBy(() -> request.markReceived(BigDecimal.TEN, null))
                .as("cannot receive before approval")
                .isInstanceOf(BusinessException.class);

        request.approve(null);
        assertThatThrownBy(request::cancel).as("customer can't cancel once approved")
                .isInstanceOf(BusinessException.class);

        request.markReceived(new BigDecimal("55.00"), "Checked");
        assertThat(request.getStatus()).isEqualTo(ReturnStatus.RECEIVED);
        assertThat(request.getRefundAmount()).isEqualByComparingTo("55.00");
        assertThat(request.getReceivedAt()).isNotNull();
        assertThatThrownBy(() -> request.reject("late")).isInstanceOf(BusinessException.class);
    }

    @Test
    void itemFromAnotherOrderIsRefused() {
        OrderItem other = line("X", "10.00", 1);
        deliveredOrder(other);
        ReturnRequest request = new ReturnRequest(deliveredOrder(line("A", "50.00", 1)), new User(), "Wrong");

        assertThatThrownBy(() -> request.addItem(other, 1)).isInstanceOf(BusinessException.class);
    }
}
