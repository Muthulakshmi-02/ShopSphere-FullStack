package com.example.MyProject.Services;
import com.example.MyProject.Analytics.dto.AnalyticsResponse;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.Order;
import com.example.MyProject.Models.OrderItem;
import com.example.MyProject.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin-only revenue/best-seller reporting, built directly from real order
 * data - nothing here is estimated or mocked. Only PAID orders count as
 * revenue (a PENDING or CANCELLED order isn't real income), matching the
 * same payment-status logic the rest of the app already uses.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;

    private static final int TOP_PRODUCTS_LIMIT = 5;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(int days) {
        int rangeDays = Math.max(1, Math.min(days, 365)); // sane bounds - avoid a 0 or absurdly large range
        LocalDateTime since = LocalDateTime.now().minusDays(rangeDays - 1L).toLocalDate().atStartOfDay();

        List<Order> paidOrders = orderRepository.findByPaymentStatusAndOrderStatusNotAndOrderDateAfter(
                PaymentStatus.PAID, com.example.MyProject.Enum.OrderStatus.CANCELLED, since);

        double totalRevenue = paidOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        int totalOrders = paidOrders.size();
        double averageOrderValue = totalOrders == 0 ? 0.0 : totalRevenue / totalOrders;

        return AnalyticsResponse.builder()
                .rangeDays(rangeDays)
                .totalRevenue(round2(totalRevenue))
                .totalOrders(totalOrders)
                .averageOrderValue(round2(averageOrderValue))
                .revenueByDay(buildDailyRevenue(paidOrders, rangeDays))
                .topProducts(buildTopProducts(paidOrders))
                .build();
    }

    /**
     * Groups paid orders by calendar day and fills in every day in the
     * range with zero revenue if nothing sold that day - a chart with
     * missing days looks like broken data, not "no sales that day".
     */
    private List<AnalyticsResponse.DailyRevenuePoint> buildDailyRevenue(List<Order> orders, int rangeDays) {
        Map<LocalDate, double[]> byDay = new HashMap<>(); // [revenue, orderCount]
        for (Order order : orders) {
            LocalDate day = order.getOrderDate().toLocalDate();
            double[] bucket = byDay.computeIfAbsent(day, d -> new double[2]);
            bucket[0] += order.getTotalAmount();
            bucket[1] += 1;
        }

        List<AnalyticsResponse.DailyRevenuePoint> points = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(rangeDays - 1L);
        for (int i = 0; i < rangeDays; i++) {
            LocalDate day = start.plusDays(i);
            double[] bucket = byDay.getOrDefault(day, new double[2]);
            points.add(AnalyticsResponse.DailyRevenuePoint.builder()
                    .date(day)
                    .revenue(round2(bucket[0]))
                    .orderCount((int) bucket[1])
                    .build());
        }
        return points;
    }

    /** Top-selling products by revenue, aggregated across every line item in every paid order in range. */
    private List<AnalyticsResponse.TopProductPoint> buildTopProducts(List<Order> orders) {
        Map<Long, Integer> unitsByProduct = new HashMap<>();
        Map<Long, Double> revenueByProduct = new HashMap<>();
        Map<Long, String> nameByProduct = new HashMap<>();

        for (Order order : orders) {
            List<OrderItem> items = order.getOrderItems();
            if (items == null) continue;
            for (OrderItem item : items) {
                if (item.getProduct() == null) continue; // defensive - a deleted product's history still has the order, but no live product row
                Long productId = item.getProduct().getProductId();
                nameByProduct.putIfAbsent(productId, item.getProduct().getName());
                unitsByProduct.merge(productId, item.getQuantity(), Integer::sum);
                revenueByProduct.merge(productId, item.getPrice() * item.getQuantity(), Double::sum);
            }
        }

        return revenueByProduct.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(entry -> AnalyticsResponse.TopProductPoint.builder()
                        .productId(entry.getKey())
                        .productName(nameByProduct.get(entry.getKey()))
                        .unitsSold(unitsByProduct.get(entry.getKey()))
                        .revenue(round2(entry.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
