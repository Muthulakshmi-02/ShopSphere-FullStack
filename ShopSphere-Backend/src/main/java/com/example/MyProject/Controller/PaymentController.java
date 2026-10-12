package com.example.MyProject.Controller;

import com.example.MyProject.Payment.dto.*;
import com.example.MyProject.Services.PaymentService;
import jakarta.validation.Valid;
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

    /** Step 1: creates (or reuses) the Razorpay order for an internal order. */
    @PostMapping("/razorpay/create-order/{orderId}")
    public ResponseEntity<RazorpayOrderResponse> createRazorpayOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(orderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@Valid @RequestBody PaymentRequest request) {
        // verifyAndSavePayment throws unless the Razorpay signature checks out, so reaching the
        // next line means success. The message used to depend on request.getStatus(), a value
        // the CLIENT sends, which could make a verified payment report "Payment failed".
        PaymentResponse response = paymentService.verifyAndSavePayment(request);
        response.setMessage(messageSource.getMessage("payment.success", null, LocaleContextHolder.getLocale()));
        return ResponseEntity.ok(response);
    }
}