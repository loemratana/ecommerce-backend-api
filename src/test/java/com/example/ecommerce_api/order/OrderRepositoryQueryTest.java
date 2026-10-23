package com.example.ecommerce_api.order;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.Enum.ReturnStatus;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.returns.repository.ReturnItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** Runs the admin order queries against the real database to catch binding problems (e.g. a null enum). */
@SpringBootTest
@Transactional
class OrderRepositoryQueryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnItemRepository returnItemRepository;

    @Test
    void searchAcceptsNullAndConcreteStatus() {
        var pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "orderDate"));

        assertThatNoException().isThrownBy(() -> orderRepository.search(null, "%a%", pageable));
        assertThatNoException().isThrownBy(() -> orderRepository.search(OrderStatus.PENDING, "%a%", pageable));
    }

    @Test
    void returnableOrderQueriesRun() {
        assertThatNoException().isThrownBy(() -> orderRepository.findReturnCandidates(
                1L, OrderStatus.DELIVERED, LocalDateTime.now().minusDays(30)));
        assertThatNoException().isThrownBy(() -> returnItemRepository.sumQuantityByOrderItemForOrders(
                List.of(1L, 2L), ReturnStatus.open()));
    }

    @Test
    void statsQueriesRun() {
        assertThatNoException().isThrownBy(() -> orderRepository.countGroupedByStatus());
        assertThat(orderRepository.sumTotalByStatusIn(EnumSet.of(OrderStatus.PAID, OrderStatus.DELIVERED))).isNotNull();
    }
}
