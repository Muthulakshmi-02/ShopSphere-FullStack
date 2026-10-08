//package com.example.MyProject.Services;
//
//import com.example.MyProject.Enum.PaymentStatus;
//import com.example.MyProject.Payment.dto.*;
//import com.example.MyProject.Models.*;
//
//import com.example.MyProject.Repository.OrderRepository;
//import com.example.MyProject.Repository.PaymentRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PaymentService {
//
//    private final PaymentRepository paymentRepository;
//    private final OrderRepository orderRepository;
//    private final RazorpayService razorpayService;
//
//    /**
//     * Step 1 of the real Razorpay flow: ask Razorpay to create an order for
//     * the amount our own order actually totals to (never trust a client-
//     * supplied amount here), then hand back what the frontend needs to open
//     * the Checkout widget. Step 2 is verifyAndSavePayment() below, once the
//     * customer actually completes payment in that widget.
//     */
//    @Transactional
//    public RazorpayOrderResponse createRazorpayOrder(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//
//        String callerEmail = getAuthenticatedEmail();
//        if (!order.getUser().getEmail().equalsIgnoreCase(callerEmail)) {
//            throw new org.springframework.security.access.AccessDeniedException(
//                    "You are not authorized to pay for this order");
//        }
//
//        if (order.getPaymentStatus() == PaymentStatus.PAID) {
//            throw new IllegalStateException("This order has already been paid for");
//        }
//
//        try {
//            String receipt = "order_" + order.getOrderId();
//            String razorpayOrderId = razorpayService.createOrder(
//                    java.math.BigDecimal.valueOf(order.getTotalAmount()), receipt);
//
//            return RazorpayOrderResponse.builder()
//                    .razorpayOrderId(razorpayOrderId)
//                    .amountInPaise(Math.round(order.getTotalAmount() * 100))
//                    .currency("INR")
//                    .keyId(razorpayService.getPublicKeyId())
//                    .orderReceipt(receipt)
//                    .build();
//        } catch (com.razorpay.RazorpayException e) {
//            // The customer-facing message stays generic on purpose (never
//            // leak raw gateway error details), but log the real cause here
//            // so it's actually visible to you - the most common cause by
//            // far is the razorpay.key.id/key.secret placeholders in
//            // application.properties not having been replaced with real
//            // test keys yet.
//            log.error("Razorpay order creation failed for order {}: {}", order.getOrderId(), e.getMessage(), e);
//            throw new com.example.MyProject.Exception.CartBusinessException(
//                    "Could not start payment right now. Please try again in a moment.");
//        }
//    }
//
//    @Transactional
//    public PaymentResponse verifyAndSavePayment(PaymentRequest request) {
//
//        Order order = orderRepository.findById(request.getOrderId())
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//
//        // SECURITY FIX (IDOR): make sure the caller actually owns this order
//        // before letting them attach/alter a payment record on it.
//        String callerEmail = getAuthenticatedEmail();
//        if (!order.getUser().getEmail().equalsIgnoreCase(callerEmail)) {
//            throw new org.springframework.security.access.AccessDeniedException(
//                    "You are not authorized to verify payment for this order");
//        }
//
//        // SECURITY FIX: don't let an already-paid order be re-verified/flipped
//        // (prevents a stale or replayed request from overwriting the status).
//        if (order.getPaymentStatus() == PaymentStatus.PAID) {
//            throw new IllegalStateException("This order has already been paid for");
//        }
//
//        // REAL VERIFICATION: for Razorpay, never trust the client's claimed
//        // "status" - actually verify the cryptographic signature Razorpay
//        // returned, using the secret key that only the backend has. This
//        // is the whole point of a real gateway integration: a client could
//        // otherwise just POST {status: "SUCCESS"} without ever paying.
//        boolean isReallyPaid;
//        if ("RAZORPAY".equalsIgnoreCase(request.getPaymentGateway())) {
//            isReallyPaid = razorpayService.verifySignature(
//                    request.getRazorpayOrderId(),
//                    request.getRazorpayPaymentId(),
//                    request.getRazorpaySignature()
//            );
//            if (isReallyPaid) {
//                // Use Razorpay's own payment id as the transaction id of
//                // record, rather than whatever the client sent.
//                request.setTransactionId(request.getRazorpayPaymentId());
//            }
//        } else {
//            // Non-gateway flows (Cash on Delivery) have nothing to
//            // cryptographically verify - the client's status is all there
//            // is, same as before.
//            isReallyPaid = "SUCCESS".equalsIgnoreCase(request.getStatus());
//        }
//
//        PaymentDetail payment = PaymentDetail.builder()
//                .order(order)
//                .transactionId(request.getTransactionId())
//                .paymentGateway(request.getPaymentGateway())
//                .paymentMethod(request.getPaymentMethod())
//                .amount(order.getTotalAmount())
//                .status(isReallyPaid ? "COMPLETED" : "FAILED")
//                .build();
//        paymentRepository.save(payment);
//
//        if (isReallyPaid) {
//            order.setPaymentStatus(PaymentStatus.PAID);
//            orderRepository.save(order);
//        } else {
//            throw new IllegalStateException("Payment verification failed. Please try again or contact support.");
//        }
//
//        List<ProductSummary> items = order.getOrderItems().stream()
//                .map(item -> ProductSummary.builder()
//                        .name(item.getProduct().getName())
//                        .quantity(item.getQuantity())
//                        .price(item.getPrice())
//                        .build())
//                .collect(Collectors.toList());
//
//        return PaymentResponse.builder()
//                .transactionId(payment.getTransactionId())
//                .totalAmount(order.getTotalAmount())
//                .status(payment.getStatus())
//                .items(items)
//                .build();
//    }
//
//    private String getAuthenticatedEmail() {
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        return (principal instanceof UserDetails)
//                ? ((UserDetails) principal).getUsername()
//                : principal.toString();
//    }
//}

package com.example.MyProject.Services;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.Order;
import com.example.MyProject.Models.PaymentDetail;
import com.example.MyProject.Payment.dto.*;
import com.example.MyProject.Repository.OrderRepository;
import com.example.MyProject.Repository.PaymentRepository;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;
    private final ApplicationEventPublisher events;

    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        assertOwner(order, "pay for");

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("This order has already been paid for");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("This order was cancelled and can no longer be paid");
        }
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new IllegalStateException("This is a Cash on Delivery order; no online payment is needed");
        }

        String receipt = "order_" + order.getOrderId();
        long amountInPaise = Math.round(order.getTotalAmount() * 100);

        // Reuse the Razorpay order if one already exists for this order: clicking "Pay"
        // twice must not create a second one (a payment against an older one could
        // otherwise never be matched back to this order).
        String razorpayOrderId = order.getRazorpayOrderId();
        try {
            if (razorpayOrderId == null) {
                razorpayOrderId = razorpayService.createOrder(BigDecimal.valueOf(order.getTotalAmount()), receipt);
                order.setRazorpayOrderId(razorpayOrderId);
                orderRepository.save(order);
            }
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}", order.getOrderId(), e.getMessage(), e);
            throw new IllegalArgumentException("Could not start payment right now. Please check your Razorpay credentials.");
        }

        return RazorpayOrderResponse.builder()
                .razorpayOrderId(razorpayOrderId)
                .amountInPaise(amountInPaise)
                .currency("INR")
                .keyId(razorpayService.getPublicKeyId())
                .orderReceipt(receipt)
                .build();
    }

    // Browser callback path. noRollbackFor so a FAILED attempt is still recorded.
    @Transactional(noRollbackFor = IllegalStateException.class)
    public PaymentResponse verifyAndSavePayment(PaymentRequest request) {
        // Row lock: the webhook may be processing the same order at the same moment.
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + request.getOrderId()));

        assertOwner(order, "verify payment for");

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("This order has already been paid for");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("This order was cancelled and can no longer be paid");
        }
        if (!"RAZORPAY".equalsIgnoreCase(request.getPaymentGateway())) {
            throw new IllegalStateException("Unsupported payment gateway");
        }
        if (order.getRazorpayOrderId() == null
                || !order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new IllegalStateException("Payment does not match this order");
        }

        boolean isReallyPaid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!isReallyPaid) {
            savePaymentRecord(order, request.getTransactionId(), request.getPaymentMethod(), "FAILED");
            throw new IllegalStateException("Payment verification failed. Please try again or contact support.");
        }

        PaymentDetail payment = savePaymentRecord(order, request.getRazorpayPaymentId(),
                request.getPaymentMethod(), "COMPLETED");
        markPaid(order, request.getRazorpayPaymentId());
        orderRepository.save(order);

        List<ProductSummary> items = order.getOrderItems().stream()
                .map(item -> ProductSummary.builder()
                        .name(item.getProduct() != null ? item.getProduct().getName() : "Product")
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return PaymentResponse.builder()
                .transactionId(payment.getTransactionId())
                .totalAmount(order.getTotalAmount())
                .status(payment.getStatus())
                .items(items)
                .build();
    }

    /**
     * Webhook path (payment.captured / order.paid). Safe to call repeatedly:
     * Razorpay retries, and the browser callback may have already done the work.
     */
    @Transactional
    public void handleCapturedPayment(String razorpayOrderId, String razorpayPaymentId,
                                      long amountPaise, String method) {
        Order order = orderRepository.findByRazorpayOrderIdForUpdate(razorpayOrderId).orElse(null);
        if (order == null) {
            log.warn("Webhook: no order found for Razorpay order {} (payment {}). Reconcile manually.",
                    razorpayOrderId, razorpayPaymentId);
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.REFUND_PENDING) {
            log.info("Webhook: order {} already handled ({}), ignoring", order.getOrderId(), order.getPaymentStatus());
            return;
        }

        long expected = Math.round(order.getTotalAmount() * 100);
        if (expected != amountPaise) {
            log.error("Webhook: amount mismatch on order {} (expected {} paise, got {}). NOT marking paid.",
                    order.getOrderId(), expected, amountPaise);
            return;
        }

        savePaymentRecord(order, razorpayPaymentId, method == null ? "ONLINE" : method.toUpperCase(), "COMPLETED");

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            // Money was captured for an order we already cancelled (stock restored).
            // We cannot fulfil it, so flag it for refund.
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            log.warn("Webhook: payment {} captured for CANCELLED order {}. Marked REFUND_PENDING - refund it in Razorpay.",
                    razorpayPaymentId, order.getOrderId());
        } else {
            markPaid(order, razorpayPaymentId);
            log.info("Webhook: order {} marked PAID via payment {}", order.getOrderId(), razorpayPaymentId);
        }
        orderRepository.save(order);
    }

    /** Shared by the browser and webhook paths: PAID, PENDING -> CONFIRMED, email after commit. */
    private void markPaid(Order order, String razorpayPaymentId) {
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setRazorpayPaymentId(razorpayPaymentId);
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
        }
        events.publishEvent(new OrderEmailEvent(order.getOrderId(), OrderEmailEvent.Type.CONFIRMATION));
    }

    private PaymentDetail savePaymentRecord(Order order, String transactionId, String method, String status) {
        return paymentRepository.save(PaymentDetail.builder()
                .order(order)
                .transactionId(transactionId)
                .paymentGateway("RAZORPAY")
                .paymentMethod(method)
                .amount(order.getTotalAmount())
                .status(status)
                .build());
    }

    private void assertOwner(Order order, String action) {
        String callerEmail = getAuthenticatedEmail();
        if (order.getUser() == null || !order.getUser().getEmail().equalsIgnoreCase(callerEmail)) {
            throw new AccessDeniedException("You are not authorized to " + action + " this order");
        }
    }

    private String getAuthenticatedEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
    }
}