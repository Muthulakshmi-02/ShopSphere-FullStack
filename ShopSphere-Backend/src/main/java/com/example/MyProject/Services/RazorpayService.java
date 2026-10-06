//package com.example.MyProject.Services;
//
//import com.razorpay.RazorpayClient;
//import com.razorpay.RazorpayException;
//import com.razorpay.Utils;
//import org.json.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//
///**
// * Talks to Razorpay's actual API - this is the real integration, not the
// * mock gateway that used to sit here. Two responsibilities:
// *   1. createOrder() - asks Razorpay to create an order (required before
// *      the frontend can open the Checkout widget for it)
// *   2. verifySignature() - the critical security step. Razorpay's
// *      checkout widget runs entirely in the browser, so a malicious client
// *      could just claim "payment succeeded" without actually paying. This
// *      HMAC signature (computed server-side, using the secret key that
// *      never touches the browser) is what actually proves a payment is
// *      real - never trust a client-reported "success" status alone.
// */
//@Service
//public class RazorpayService {
//
//    @Value("${razorpay.key.id}")
//    private String keyId;
//
//    @Value("${razorpay.key.secret}")
//    private String keySecret;
//
//    /**
//     * Creates a Razorpay order for the given amount. Amount must be in the
//     * smallest currency unit (paise for INR - e.g. ₹499.00 is 49900).
//     */
//    public String createOrder(BigDecimal amountInRupees, String receipt) throws RazorpayException {
//        RazorpayClient client = new RazorpayClient(keyId, keySecret);
//
//        long amountInPaise = amountInRupees
//                .multiply(BigDecimal.valueOf(100))
//                .longValueExact();
//
//        JSONObject orderRequest = new JSONObject();
//        orderRequest.put("amount", amountInPaise);
//        orderRequest.put("currency", "INR");
//        orderRequest.put("receipt", receipt);
//        // Auto-capture immediately on successful authorization, rather than
//        // requiring a separate manual capture step - simplest correct
//        // default for a standard checkout flow.
//        orderRequest.put("payment_capture", 1);
//
//        com.razorpay.Order order = client.orders.create(orderRequest);
//        return order.get("id");
//    }
//
//    /**
//     * Verifies that a completed-payment callback actually came from
//     * Razorpay and wasn't fabricated by the client. This is the real
//     * fix for the mock flow's biggest gap: previously the backend just
//     * trusted whatever "status" the client sent.
//     */
//    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
//        try {
//            JSONObject payload = new JSONObject();
//            payload.put("razorpay_order_id", razorpayOrderId);
//            payload.put("razorpay_payment_id", razorpayPaymentId);
//            payload.put("razorpay_signature", razorpaySignature);
//            return Utils.verifyPaymentSignature(payload, keySecret);
//        } catch (RazorpayException e) {
//            return false;
//        }
//    }
//
//    public String getPublicKeyId() {
//        return keyId;
//    }
////}
//package com.example.MyProject.Services;
//
//import com.razorpay.RazorpayClient;
//import com.razorpay.RazorpayException;
//import com.razorpay.Utils;
//import org.json.JSONObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//
//@Service
//public class RazorpayService {
//
//    @Value("${razorpay.key.id}")
//    private String keyId;
//
//    @Value("${razorpay.key.secret}")
//    private String keySecret;
//
//    /**
//     * Creates a Razorpay order for the given amount in Rupees.
//     * Converts rupees to paise (smallest currency unit) automatically.
//     */
//    public String createOrder(BigDecimal amountInRupees, String receipt) throws RazorpayException {
//        RazorpayClient client = new RazorpayClient(keyId, keySecret);
//
//        // Convert Rupees to Paise safely (e.g. 1000.00 -> 100000)
//        long amountInPaise = amountInRupees
//                .multiply(BigDecimal.valueOf(100))
//                .longValue();
//
//        JSONObject orderRequest = new JSONObject();
//        orderRequest.put("amount", amountInPaise);
//        orderRequest.put("currency", "INR");
//        orderRequest.put("receipt", receipt);
//
//        // Note: Razorpay API v1 uses string/boolean or payment_capture integer
//        orderRequest.put("payment_capture", 1);
//
//        com.razorpay.Order order = client.orders.create(orderRequest);
//        return order.get("id");
//    }
//
//    /**
//     * Verifies HMAC signature returned by Razorpay Checkout
//     */
//    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
//        try {
//            JSONObject payload = new JSONObject();
//            payload.put("razorpay_order_id", razorpayOrderId);
//            payload.put("razorpay_payment_id", razorpayPaymentId);
//            payload.put("razorpay_signature", razorpaySignature);
//            return Utils.verifyPaymentSignature(payload, keySecret);
//        } catch (RazorpayException e) {
//            return false;
//        }
//    }
//
//    public String getPublicKeyId() {
//        return keyId;
//    }
//}

package com.example.MyProject.Services;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
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

    /** Creates a Razorpay order for an amount in Rupees (converted to paise). */
    public String createOrder(BigDecimal amountInRupees, String receipt) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        long amountInPaise = amountInRupees
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValue();

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
        try {
            JSONObject payload = new JSONObject();
            payload.put("razorpay_order_id", razorpayOrderId);
            payload.put("razorpay_payment_id", razorpayPaymentId);
            payload.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(payload, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    /** Verifies the X-Razorpay-Signature header of a webhook against the RAW request body. */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
        } catch (RazorpayException e) {
            return false;
        }
    }

    public String getPublicKeyId() {
        return keyId;
    }
}