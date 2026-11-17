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

    // Separate from the API key secret: chosen when creating the webhook in the Razorpay dashboard.
    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    private RazorpayClient client;

    @PostConstruct
    void init() throws RazorpayException {
        client = new RazorpayClient(keyId, keySecret);
    }

    /** Creates a Razorpay order for an amount in Rupees (converted to paise). */
    public String createOrder(BigDecimal amountInRupees, String receipt) throws RazorpayException {
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

    /** Verifies the X-Razorpay-Signature header of a webhook against the RAW request body. */
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

    /**
     * Refunds a captured payment (the whole amount, in paise) and returns Razorpay's refund id.
     */
    public String refundPayment(String paymentId, long amountInPaise, String receipt) throws RazorpayException {
        JSONObject request = new JSONObject();
        request.put("amount", amountInPaise);
        request.put("speed", "normal");
        request.put("receipt", receipt);

        com.razorpay.Refund refund = client.payments.refund(paymentId, request);
        return refund.get("id");
    }

    public String getPublicKeyId() {
        return keyId;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
