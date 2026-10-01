package com.example.MyProject.Controller;

import com.example.MyProject.Payment.dto.*;
import com.example.MyProject.Services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MessageSource messageSource;

    /**
     * Step 1: creates a real Razorpay order for the given internal order,
     * returning what the frontend needs to open the Checkout widget.
     */
    @PostMapping("/razorpay/create-order/{orderId}")
    public ResponseEntity<RazorpayOrderResponse> createRazorpayOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(orderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.verifyAndSavePayment(request);
        String messageKey = "SUCCESS".equalsIgnoreCase(request.getStatus()) ? "payment.success" : "payment.failed";
        String localizedMessage = messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
        response.setMessage(localizedMessage);
        return ResponseEntity.ok(response);
    }
}