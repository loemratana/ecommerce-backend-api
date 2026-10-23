package com.example.ecommerce_api.customer;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.customer.service.CustomerService;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** Runs the customer queries against the real database to catch JPQL and binding problems (e.g. a null Boolean). */
@SpringBootTest
@Transactional
class CustomerQueryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerService customerService;

    @Test
    void searchCustomersAcceptsNullAndConcreteActive() {
        var pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThatNoException().isThrownBy(() -> userRepository.searchCustomers(null, "%%", pageable));
        assertThatNoException().isThrownBy(() -> userRepository.searchCustomers(true, "%a%", pageable));
    }

    @Test
    void perCustomerOrderQueriesRun() {
        List<Long> ids = List.of(1L, 2L);
        assertThatNoException().isThrownBy(() -> orderRepository.countAndLastOrderByUserIds(ids));
        assertThatNoException().isThrownBy(() -> orderRepository.sumTotalByUserIds(ids, OrderStatus.revenueStatuses()));
        assertThatNoException().isThrownBy(() -> orderRepository.countGroupedByStatusForUser(1L));
    }

    @Test
    void serviceListAndStatsRun() {
        var page = customerService.getAll(null, null, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertThat(page.getContent()).isNotNull();

        var stats = customerService.getStats();
        assertThat(stats.getActiveCustomers() + stats.getInactiveCustomers()).isEqualTo(stats.getTotalCustomers());

        // Detail for the first customer, if the database has one.
        page.getContent().stream().findFirst().ifPresent(customer ->
                assertThat(customerService.getById(customer.getId()).getId()).isEqualTo(customer.getId()));
    }
}
