package com.example.MyProject.Payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order id is required.")
    private Long orderId;

    private String transactionId;
    private String paymentMethod;

    @NotBlank(message = "Payment gateway is required.")
    private String paymentGateway; // only "RAZORPAY" is accepted by PaymentService

    // Ignored by the server: the payment is trusted only after the Razorpay
    // signature is verified.
    private String status;

    // Sent by Razorpay's checkout callback in the browser; verified
    // cryptographically server-side.
    // They used to be optional, so a missing one caused a 500 deep inside the
    // signature check.
    @NotBlank(message = "Razorpay order id is required.")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment id is required.")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required.")
    private String razorpaySignature;
}