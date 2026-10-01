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

        try {
            String receipt = "order_" + order.getOrderId();
            BigDecimal amountInRupees = BigDecimal.valueOf(order.getTotalAmount());

            String razorpayOrderId = razorpayService.createOrder(amountInRupees, receipt);

            // Remember WHICH Razorpay order belongs to this order, so verify can
            // refuse a payment made against a different (cheaper) Razorpay order.
            order.setRazorpayOrderId(razorpayOrderId);
            orderRepository.save(order);

            return RazorpayOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amountInPaise(Math.round(order.getTotalAmount() * 100))
                    .currency("INR")
                    .keyId(razorpayService.getPublicKeyId())
                    .orderReceipt(receipt)
                    .build();
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}", order.getOrderId(), e.getMessage(), e);
            throw new IllegalArgumentException("Could not start payment right now. Please check your Razorpay credentials.");
        }
    }

    // noRollbackFor: so the FAILED payment record is actually saved before we throw.
    @Transactional(noRollbackFor = IllegalStateException.class)
    public PaymentResponse verifyAndSavePayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + request.getOrderId()));

        assertOwner(order, "verify payment for");

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("This order has already been paid for");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("This order was cancelled and can no longer be paid");
        }

        // Only Razorpay is supported. The old "else: trust request.status" branch let
        // anyone mark an order PAID without paying, so it is gone.
        if (!"RAZORPAY".equalsIgnoreCase(request.getPaymentGateway())) {
            throw new IllegalStateException("Unsupported payment gateway");
        }

        // The Razorpay order in the proof must be the one we created for THIS order.
        if (order.getRazorpayOrderId() == null
                || !order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new IllegalStateException("Payment does not match this order");
        }

        boolean isReallyPaid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        PaymentDetail payment = PaymentDetail.builder()
                .order(order)
                .transactionId(isReallyPaid ? request.getRazorpayPaymentId() : request.getTransactionId())
                .paymentGateway("RAZORPAY")
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalAmount())
                .status(isReallyPaid ? "COMPLETED" : "FAILED")
                .build();
        paymentRepository.save(payment);

        if (!isReallyPaid) {
            throw new IllegalStateException("Payment verification failed. Please try again or contact support.");
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);   // confirmed only once paid
        }
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

    private void assertOwner(Order order, String action) {
        String callerEmail = getAuthenticatedEmail();
        // A missing user is now rejected instead of silently skipping the check.
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