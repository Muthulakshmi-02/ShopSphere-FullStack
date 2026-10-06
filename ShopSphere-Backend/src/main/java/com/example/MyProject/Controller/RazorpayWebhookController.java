package com.example.MyProject.Controller;

import com.example.MyProject.Services.PaymentService;
import com.example.MyProject.Services.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * Receives Razorpay's server-to-server notifications. Must be public in
 * SecurityConfig (Razorpay has no JWT); the SIGNATURE is the authentication.
 */
@RestController
@RequestMapping("/api/payments/razorpay")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final RazorpayService razorpayService;
    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody byte[] body,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        // The signature is computed over the exact raw bytes, so never re-serialise the JSON.
        String payload = new String(body, StandardCharsets.UTF_8);

        if (signature == null || !razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("Rejected Razorpay webhook: missing/invalid signature (or razorpay.webhook.secret not set)");
            return ResponseEntity.status(400).body("invalid signature");
        }

        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event");

            if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                JSONObject payment = json.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                paymentService.handleCapturedPayment(
                        payment.getString("order_id"),
                        payment.getString("id"),
                        payment.getLong("amount"),
                        payment.optString("method", "ONLINE"));
            } else {
                log.info("Ignoring Razorpay webhook event '{}'", event);
            }
            return ResponseEntity.ok("ok");

        } catch (Exception e) {
            // 5xx makes Razorpay retry later, which is what we want for a transient failure.
            log.error("Razorpay webhook processing failed", e);
            return ResponseEntity.status(500).body("error");
        }
    }
}