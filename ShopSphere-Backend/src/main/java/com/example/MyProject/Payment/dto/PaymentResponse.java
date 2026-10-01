package com.example.MyProject.Payment.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private String message;
    private String transactionId;
    private Double totalAmount;
    private String status;
    private List<ProductSummary> items; // List of products from the order
}

