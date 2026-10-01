package com.example.MyProject.Analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AnalyticsResponse {
    private int rangeDays;
    private double totalRevenue;
    private int totalOrders;
    private double averageOrderValue;
    private List<DailyRevenuePoint> revenueByDay;
    private List<TopProductPoint> topProducts;

    @Getter
    @Builder
    public static class DailyRevenuePoint {
        private LocalDate date;
        private double revenue;
        private int orderCount;
    }

    @Getter
    @Builder
    public static class TopProductPoint {
        private Long productId;
        private String productName;
        private int unitsSold;
        private double revenue;
    }
}
