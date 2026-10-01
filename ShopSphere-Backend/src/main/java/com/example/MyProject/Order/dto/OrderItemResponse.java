package com.example.MyProject.Order.dto;

import lombok.Data;

@Data
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double priceAtPurchase;
    private String imageUrl;
    // Null for simple (non-variant) products - e.g. "Size: 9, Color: Black"
    // when the purchased item was a specific variant.
    private String variantLabel;
}