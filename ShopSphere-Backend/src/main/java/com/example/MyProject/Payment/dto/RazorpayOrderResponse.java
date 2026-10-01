package com.example.MyProject.Payment.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Everything the frontend needs to open Razorpay's Checkout widget for a
 * specific order. keyId is safe to expose to the browser (it's the public
 * identifier); the secret key never leaves the backend.
 */
@Data
@Builder
public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private long amountInPaise;
    private String currency;
    private String keyId;
    private String orderReceipt;
}
