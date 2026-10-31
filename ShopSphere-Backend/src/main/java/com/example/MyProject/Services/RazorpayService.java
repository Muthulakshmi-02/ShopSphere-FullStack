
// package com.example.MyProject.Services;

// import com.razorpay.RazorpayClient;
// import com.razorpay.RazorpayException;
// import com.razorpay.Utils;
// import org.json.JSONObject;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import java.math.BigDecimal;
// import java.math.RoundingMode;

// @Service
// public class RazorpayService {

//     @Value("${razorpay.key.id}")
//     private String keyId;

//     @Value("${razorpay.key.secret}")
//     private String keySecret;

//     // Separate from the API key secret: you choose it when creating the webhook
//     // in the Razorpay dashboard. Empty = every webhook is rejected.
//     @Value("${razorpay.webhook.secret:}")
//     private String webhookSecret;

//     /** Creates a Razorpay order for an amount in Rupees (converted to paise). */
//     public String createOrder(BigDecimal amountInRupees, String receipt) throws RazorpayException {
//         RazorpayClient client = new RazorpayClient(keyId, keySecret);

//         long amountInPaise = amountInRupees
//                 .setScale(2, RoundingMode.HALF_UP)
//                 .multiply(BigDecimal.valueOf(100))
//                 .longValue();

//         JSONObject orderRequest = new JSONObject();
//         orderRequest.put("amount", amountInPaise);
//         orderRequest.put("currency", "INR");
//         orderRequest.put("receipt", receipt);
//         orderRequest.put("payment_capture", 1);

//         com.razorpay.Order order = client.orders.create(orderRequest);
//         return order.get("id");
//     }

//     /** Verifies the HMAC signature returned to the browser by Razorpay Checkout. */
//     public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
//         try {
//             JSONObject payload = new JSONObject();
//             payload.put("razorpay_order_id", razorpayOrderId);
//             payload.put("razorpay_payment_id", razorpayPaymentId);
//             payload.put("razorpay_signature", razorpaySignature);
//             return Utils.verifyPaymentSignature(payload, keySecret);
//         } catch (RazorpayException e) {
//             return false;
//         }
//     }

//     /** Verifies the X-Razorpay-Signature header of a webhook against the RAW request body. */
//     public boolean verifyWebhookSignature(String rawBody, String signature) {
//         if (webhookSecret == null || webhookSecret.isBlank()) {
//             return false;
//         }
//         try {
//             return Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
//         } catch (RazorpayException e) {
//             return false;
//         }
//     }

//     public String getPublicKeyId() {
//         return keyId;
//     }
// }

package com.example.MyProject.Services;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // Separate from the API key secret: you choose it when creating the webhook
    // in the Razorpay dashboard. Empty = every webhook is rejected.
    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    // CHANGED: one client for the whole app instead of a new one on every payment.
    private RazorpayClient client;

    @PostConstruct
    void init() throws RazorpayException {
        client = new RazorpayClient(keyId, keySecret);
    }

    /** Creates a Razorpay order for an amount in Rupees (converted to paise). */
    public String createOrder(BigDecimal amountInRupees, String receipt) throws RazorpayException {
        // CHANGED: longValueExact() fails loudly instead of silently truncating a bad
        // amount.
        long amountInPaise = amountInRupees
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt);
        orderRequest.put("payment_capture", 1);

        com.razorpay.Order order = client.orders.create(orderRequest);
        return order.get("id");
    }

    /** Verifies the HMAC signature returned to the browser by Razorpay Checkout. */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        // CHANGED: a missing value used to make JSONObject throw a runtime exception (a
        // 500).
        // Now it is simply "not verified".
        if (isBlank(razorpayOrderId) || isBlank(razorpayPaymentId) || isBlank(razorpaySignature)) {
            return false;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("razorpay_order_id", razorpayOrderId);
            payload.put("razorpay_payment_id", razorpayPaymentId);
            payload.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(payload, keySecret);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifies the X-Razorpay-Signature header of a webhook against the RAW request
     * body.
     */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank() || isBlank(signature)) {
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
        } catch (Exception e) {
            return false;
        }
    }

    public String getPublicKeyId() {
        return keyId;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}