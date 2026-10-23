package com.example.ecommerce_api.dashboard.services;

import com.example.ecommerce_api.Enum.OrderStatus;
import com.example.ecommerce_api.dashboard.dto.SalesBucketResponse;
import com.example.ecommerce_api.dashboard.dto.SalesOverviewResponse;
import com.example.ecommerce_api.order.dto.OrderStatsResponse;
import com.example.ecommerce_api.order.repository.OrderRepository;
import com.example.ecommerce_api.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService{


    private static final Set<OrderStatus>  PAID  = OrderStatus.revenueStatuses();

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    @Override
    public SalesOverviewResponse getSalesOverview(Integer year) {

        LocalDate today = LocalDate.now();
        int reportYear  = year !=null ? year : today.getYear();

        List<SalesBucketResponse> month = monthlyBuckets(reportYear);
        BigDecimal yearRevenue = month.stream().map(SalesBucketResponse::getRevenue)
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);

        OrderStatsResponse stats = orderService.getStats();
        return SalesOverviewResponse.builder()
                .year(reportYear)
                .months(month)
                .thisMonth(monthBucket(thisMonth))
                .lastMonth(monthBucket(lastMonth))
                .todayRevenue(between(today, today.plusDays(1)).getRevenue())
                .yearRevenue(yearRevenue)
                .totalOrders(stats.getTotalOrders())
                .statusCounts(stats.getByStatus())
                .build();
    }

    private List<SalesBucketResponse>monthlyBuckets(int year)
    {
        long[] orders = new long[12];
        BigDecimal[] revenue = new BigDecimal[12];

        Arrays.fill(revenue,BigDecimal.ZERO);

        LocalDate from = LocalDate.of(year,1,1);

        for(Object[] row:orderRepository.sumGroupedByMonth(PAID,from.atStartOfDay(),from.plusYears(1).atStartOfDay()))
        {
            int index = ((Number) row[0]).intValue()-1;
            orders[index] = ((Number) row[1]).longValue()-1;
            revenue[index] = (BigDecimal) row[2];
        }
        List<SalesBucketResponse> months = new ArrayList<>(12);

        for ( int i= 0 ; i<12; i++)
        {
            months.add(SalesBucketResponse.builder()
                    .label(label(Month.of(i + 1)))
                    .orders(orders[i])
                    .revenue(revenue[i])
                    .build());
        }

        return months;
    }

    private SalesBucketResponse monthBucket(YearMonth month) {
        SalesBucketResponse totals = between(month.atDay(1), month.plusMonths(1).atDay(1));
        return SalesBucketResponse.builder()
                .label(label(month.getMonth()))
                .orders(totals.getOrders())
                .revenue(totals.getRevenue())
                .build();
    }

    private SalesBucketResponse between(LocalDate from ,LocalDate to)
    {
        Object[] row = orderRepository.sumBetween(PAID,from.atStartOfDay(),to.atStartOfDay()).get(0);
        return SalesBucketResponse.builder()
                .orders(((Number) row[0]).intValue())
                .revenue((BigDecimal) row[1])
                .build();
    }

    private static String label(Month month)
    {
        return month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }
}
