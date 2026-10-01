package com.example.MyProject.Order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private LocalDateTime orderDate;
    private Double totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String shippingAddress;
    private List<OrderItemResponse> items; // Detail summary of purchased items
    private String paymentMethod;
    private String promoCode;
    private Double discountAmount;
}