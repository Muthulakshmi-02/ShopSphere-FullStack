package com.example.MyProject.Payment.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private String transactionId;
    private String paymentMethod;
    private String paymentGateway; // e.g., "RAZORPAY"
    private String status;         // e.g., "SUCCESS" - only trusted for non-gateway (COD) flows now; see PaymentService

    // Populated by Razorpay's checkout callback in the browser and sent
    // here so the backend can verify them itself - required whenever
    // paymentGateway is "RAZORPAY". Never trust these without verifying
    // the signature server-side; a client could otherwise fabricate them.
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
