package com.example.MyProject.Order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long orderId;
    private LocalDateTime orderDate;
    private Double totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String shippingAddress;
    private List<OrderItemResponse> items;
    private String paymentMethod;
    private String promoCode;
    private Double discountAmount;

    // Contact info
    private String customerName;
    private String customerEmail;
    private String phoneNumber;

    // Delivery & Live Tracking
    private String trackingNumber;
    private String carrier;
    private LocalDateTime estimatedDelivery;
    private List<OrderStatusHistoryResponse> statusHistory;
}
