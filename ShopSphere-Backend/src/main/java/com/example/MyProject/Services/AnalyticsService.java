package com.example.MyProject.Services;

import com.example.MyProject.Analytics.dto.AnalyticsResponse;
import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Repository.OrderItemRepository;
import com.example.MyProject.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only revenue / best-seller reporting from real order data. Only PAID, non-cancelled
 * orders count as revenue (COD orders become PAID when delivered).
 *
 * Rewritten to ask the database for just the numbers it needs. The old version loaded EVERY
 * paid order of the period, with all their lines and products, into memory (up to 365 days),
 * which gets slower with every order placed.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private static final int TOP_PRODUCTS_LIMIT = 5;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(int days) {
        int rangeDays = Math.max(1, Math.min(days, 365)); // sane bounds
        LocalDateTime since = LocalDate.now().minusDays(rangeDays - 1L).atStartOfDay();

        List<Object[]> points = orderRepository.findPaidOrderPoints(
                PaymentStatus.PAID, OrderStatus.CANCELLED, since);

        double totalRevenue = 0.0;
        for (Object[] p : points) {
            totalRevenue += ((Number) p[1]).doubleValue();
        }
        int totalOrders = points.size();
        double averageOrderValue = totalOrders == 0 ? 0.0 : totalRevenue / totalOrders;

        return AnalyticsResponse.builder()
                .rangeDays(rangeDays)
                .totalRevenue(round2(totalRevenue))
                .totalOrders(totalOrders)
                .averageOrderValue(round2(averageOrderValue))
                .revenueByDay(buildDailyRevenue(points, rangeDays))
                .topProducts(buildTopProducts(since))
                .build();
    }

    // Groups orders by calendar day and fills EVERY day of the range (zero when nothing sold):
    // a chart with missing days looks like broken data.
    private List<AnalyticsResponse.DailyRevenuePoint> buildDailyRevenue(List<Object[]> points, int rangeDays) {
        Map<LocalDate, double[]> byDay = new HashMap<>(); // [revenue, orderCount]
        for (Object[] p : points) {
            LocalDate day = ((LocalDateTime) p[0]).toLocalDate();
            double[] bucket = byDay.computeIfAbsent(day, d -> new double[2]);
            bucket[0] += ((Number) p[1]).doubleValue();
            bucket[1] += 1;
        }

        List<AnalyticsResponse.DailyRevenuePoint> result = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(rangeDays - 1L);
        for (int i = 0; i < rangeDays; i++) {
            LocalDate day = start.plusDays(i);
            double[] bucket = byDay.getOrDefault(day, new double[2]);
            result.add(AnalyticsResponse.DailyRevenuePoint.builder()
                    .date(day)
                    .revenue(round2(bucket[0]))
                    .orderCount((int) bucket[1])
                    .build());
        }
        return result;
    }

    private List<AnalyticsResponse.TopProductPoint> buildTopProducts(LocalDateTime since) {
        List<Object[]> rows = orderItemRepository.findTopProductsByRevenue(
                PaymentStatus.PAID, OrderStatus.CANCELLED, since, PageRequest.of(0, TOP_PRODUCTS_LIMIT));

        List<AnalyticsResponse.TopProductPoint> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(AnalyticsResponse.TopProductPoint.builder()
                    .productId(((Number) r[0]).longValue())
                    .productName((String) r[1])
                    .unitsSold(((Number) r[2]).intValue())
                    .revenue(round2(((Number) r[3]).doubleValue()))
                    .build());
        }
        return result;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}