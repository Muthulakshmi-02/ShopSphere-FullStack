
// package com.example.MyProject.Services;

// import com.example.MyProject.Enum.OrderStatus;
// import com.example.MyProject.Enum.PaymentStatus;
// import com.example.MyProject.Models.Order;
// import com.example.MyProject.Models.PaymentDetail;
// import com.example.MyProject.Payment.dto.*;
// import com.example.MyProject.Repository.OrderRepository;
// import com.example.MyProject.Repository.PaymentRepository;
// import com.razorpay.RazorpayException;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.context.ApplicationEventPublisher;
// import org.springframework.security.access.AccessDeniedException;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import com.example.MyProject.Repository.CartRepository;
// import java.util.Objects;
// import java.math.BigDecimal;
// import java.util.List;
// import java.util.stream.Collectors;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class PaymentService {

//     private final PaymentRepository paymentRepository;
//     private final OrderRepository orderRepository;
//     private final CartRepository cartRepository;
//     private final RazorpayService razorpayService;
//     private final ApplicationEventPublisher events;

//     @Transactional
//     public RazorpayOrderResponse createRazorpayOrder(Long orderId) {
//         // Row lock (was a plain findById): two quick clicks on "Pay" could each create
//         // a
//         // Razorpay order, and a payment against the overwritten one could never be
//         // matched.
//         Order order = orderRepository.findByIdForUpdate(orderId)
//                 .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

//         assertOwner(order, "pay for");

//         if (order.getPaymentStatus() == PaymentStatus.PAID) {
//             throw new IllegalStateException("This order has already been paid for");
//         }
//         if (order.getOrderStatus() == OrderStatus.CANCELLED) {
//             throw new IllegalStateException("This order was cancelled and can no longer be paid");
//         }
//         if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
//             throw new IllegalStateException("This is a Cash on Delivery order; no online payment is needed");
//         }

//         String receipt = "order_" + order.getOrderId();
//         long amountInPaise = Math.round(order.getTotalAmount() * 100);

//         String razorpayOrderId = order.getRazorpayOrderId();
//         try {
//             if (razorpayOrderId == null) {
//                 razorpayOrderId = razorpayService.createOrder(BigDecimal.valueOf(order.getTotalAmount()), receipt);
//                 order.setRazorpayOrderId(razorpayOrderId);
//                 orderRepository.save(order);
//             }
//         } catch (RazorpayException e) {
//             log.error("Razorpay order creation failed for order {}: {}", order.getOrderId(), e.getMessage(), e);
//             throw new IllegalArgumentException(
//                     "Could not start payment right now. Please check your Razorpay credentials.");
//         }

//         return RazorpayOrderResponse.builder()
//                 .razorpayOrderId(razorpayOrderId)
//                 .amountInPaise(amountInPaise)
//                 .currency("INR")
//                 .keyId(razorpayService.getPublicKeyId())
//                 .orderReceipt(receipt)
//                 .build();
//     }

//     // Browser callback path.
//     @Transactional
//     public PaymentResponse verifyAndSavePayment(PaymentRequest request) {
//         Order order = orderRepository.findByIdForUpdate(request.getOrderId())
//                 .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + request.getOrderId()));

//         assertOwner(order, "verify payment for");

//         if (order.getPaymentStatus() == PaymentStatus.PAID) {
//             throw new IllegalStateException("This order has already been paid for");
//         }
//         if (order.getOrderStatus() == OrderStatus.CANCELLED) {
//             throw new IllegalStateException("This order was cancelled and can no longer be paid");
//         }
//         if (!"RAZORPAY".equalsIgnoreCase(request.getPaymentGateway())) {
//             throw new IllegalStateException("Unsupported payment gateway");
//         }
//         if (order.getRazorpayOrderId() == null
//                 || !order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
//             throw new IllegalStateException("Payment does not match this order");
//         }

//         boolean isReallyPaid = razorpayService.verifySignature(
//                 request.getRazorpayOrderId(),
//                 request.getRazorpayPaymentId(),
//                 request.getRazorpaySignature());

//         if (!isReallyPaid) {
//             // No FAILED row is stored any more. payment_details allows ONE row per order
//             // (unique
//             // order_id) and unique transaction_id, so a stored FAILED attempt made every
//             // later
//             // successful payment for the same order fail with a database error.
//             log.warn("Signature verification FAILED for order {} (razorpay order {})",
//                     order.getOrderId(), request.getRazorpayOrderId());
//             throw new IllegalStateException("Payment verification failed. Please try again or contact support.");
//         }

//         PaymentDetail payment = recordCompletedPayment(order, request.getRazorpayPaymentId(),
//                 request.getPaymentMethod());
//         markPaid(order, request.getRazorpayPaymentId());
//         orderRepository.save(order);

//         List<ProductSummary> items = order.getOrderItems().stream()
//                 .map(item -> ProductSummary.builder()
//                         .name(item.getProduct() != null ? item.getProduct().getName() : "Product")
//                         .quantity(item.getQuantity())
//                         .price(item.getPrice())
//                         .build())
//                 .collect(Collectors.toList());

//         return PaymentResponse.builder()
//                 .transactionId(payment.getTransactionId())
//                 .totalAmount(order.getTotalAmount())
//                 .status(payment.getStatus())
//                 .items(items)
//                 .build();
//     }

//     /**
//      * Webhook path (payment.captured / order.paid). Safe to call repeatedly:
//      * Razorpay retries, and the browser callback may have already done the work.
//      */
//     @Transactional
//     public void handleCapturedPayment(String razorpayOrderId, String razorpayPaymentId,
//             long amountPaise, String method) {
//         Order order = orderRepository.findByRazorpayOrderIdForUpdate(razorpayOrderId).orElse(null);
//         if (order == null) {
//             log.warn("Webhook: no order found for Razorpay order {} (payment {}). Reconcile manually.",
//                     razorpayOrderId, razorpayPaymentId);
//             return;
//         }

//         if (order.getPaymentStatus() == PaymentStatus.PAID
//                 || order.getPaymentStatus() == PaymentStatus.REFUND_PENDING) {
//             log.info("Webhook: order {} already handled ({}), ignoring", order.getOrderId(), order.getPaymentStatus());
//             return;
//         }

//         long expected = Math.round(order.getTotalAmount() * 100);
//         if (expected != amountPaise) {
//             log.error("Webhook: amount mismatch on order {} (expected {} paise, got {}). NOT marking paid.",
//                     order.getOrderId(), expected, amountPaise);
//             return;
//         }

//         recordCompletedPayment(order, razorpayPaymentId, method == null ? "ONLINE" : method.toUpperCase());

//         if (order.getOrderStatus() == OrderStatus.CANCELLED) {
//             // Money was captured for an order we already cancelled (stock restored).
//             order.setRazorpayPaymentId(razorpayPaymentId);
//             order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
//             log.warn(
//                     "Webhook: payment {} captured for CANCELLED order {}. Marked REFUND_PENDING - refund it in Razorpay.",
//                     razorpayPaymentId, order.getOrderId());
//         } else {
//             markPaid(order, razorpayPaymentId);
//             log.info("Webhook: order {} marked PAID via payment {}", order.getOrderId(), razorpayPaymentId);
//         }
//         orderRepository.save(order);
//     }

//     /**
//      * Shared by the browser and webhook paths: PAID, PENDING -> CONFIRMED, email
//      * after commit.
//      */
//     private void markPaid(Order order, String razorpayPaymentId) {
//         order.setPaymentStatus(PaymentStatus.PAID);
//         order.setRazorpayPaymentId(razorpayPaymentId);
//         if (order.getOrderStatus() == OrderStatus.PENDING) {
//             order.setOrderStatus(OrderStatus.CONFIRMED);
//         }
//         events.publishEvent(new OrderEmailEvent(order.getOrderId(), OrderEmailEvent.Type.CONFIRMATION));
//         removePurchasedLinesFromCart(order); // <-- NEW
//     }

//     // One payment row per order: update it if it exists, otherwise create it.
//     private PaymentDetail recordCompletedPayment(Order order, String transactionId, String method) {
//         PaymentDetail payment = paymentRepository.findByOrder_OrderId(order.getOrderId())
//                 .orElseGet(() -> PaymentDetail.builder()
//                         .order(order)
//                         .paymentGateway("RAZORPAY")
//                         .build());

//         payment.setTransactionId(transactionId);
//         payment.setPaymentMethod(method == null || method.isBlank() ? "ONLINE" : method);
//         payment.setAmount(order.getTotalAmount());
//         payment.setStatus("COMPLETED");
//         return paymentRepository.save(payment);
//     }

//     private void assertOwner(Order order, String action) {
//         String callerEmail = getAuthenticatedEmail();
//         if (order.getUser() == null || !order.getUser().getEmail().equalsIgnoreCase(callerEmail)) {
//             throw new AccessDeniedException("You are not authorized to " + action + " this order");
//         }
//     }

//     private String getAuthenticatedEmail() {
//         Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//         return (principal instanceof UserDetails)
//                 ? ((UserDetails) principal).getUsername()
//                 : principal.toString();
//     }

//     private void removePurchasedLinesFromCart(Order order) {
//         if (order.getUser() == null || order.getOrderItems() == null)
//             return;

//         cartRepository.findByUser_UserId(order.getUser().getUserId()).ifPresent(cart -> {
//             if (cart.getItems() == null || cart.getItems().isEmpty())
//                 return;

//             cart.getItems().removeIf(cartItem -> order.getOrderItems().stream()
//                     .anyMatch(orderItem -> orderItem.getProduct() != null
//                             && orderItem.getProduct().getProductId().equals(cartItem.getProduct().getProductId())
//                             && Objects.equals(
//                                     orderItem.getVariant() != null ? orderItem.getVariant().getVariantId() : null,
//                                     cartItem.getVariant() != null ? cartItem.getVariant().getVariantId() : null)));

//             double total = cart.getItems().stream()
//                     .mapToDouble(i -> i.getPrice() * i.getQuantity())
//                     .sum();
//             cart.setTotalAmount(Math.round(total * 100.0) / 100.0);
//             cartRepository.save(cart);
//         });
//     }
// }

package com.example.MyProject.Services;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Exception.ResourceNotFoundException;
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
import com.example.MyProject.Repository.CartRepository;
import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final RazorpayService razorpayService;
    private final ApplicationEventPublisher events;

    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(Long orderId) {
        // Row lock: two quick clicks on "Pay" must not create two Razorpay orders.
        // CHANGED: ResourceNotFoundException (404) instead of IllegalArgumentException,
        // which your
        // GlobalExceptionHandler turned into a generic 500 "Something went wrong".
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

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
        // CHANGED: one helper for paise, used for both Razorpay and the response (and
        // the webhook check).
        long amountInPaise = toPaise(order.getTotalAmount());

        String razorpayOrderId = order.getRazorpayOrderId();
        try {
            if (razorpayOrderId == null) {
                razorpayOrderId = razorpayService.createOrder(BigDecimal.valueOf(order.getTotalAmount()), receipt);
                order.setRazorpayOrderId(razorpayOrderId);
                orderRepository.save(order);
            }
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed for order {}: {}", order.getOrderId(), e.getMessage(), e);
            // CHANGED: IllegalStateException -> 409 with this message, instead of a generic
            // 500.
            throw new IllegalStateException("Could not start payment right now. Please try again in a moment.");
        }

        return RazorpayOrderResponse.builder()
                .razorpayOrderId(razorpayOrderId)
                .amountInPaise(amountInPaise)
                .currency("INR")
                .keyId(razorpayService.getPublicKeyId())
                .orderReceipt(receipt)
                .build();
    }

    // Browser callback path.
    @Transactional
    public PaymentResponse verifyAndSavePayment(PaymentRequest request) {
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + request.getOrderId()));

        assertOwner(order, "verify payment for");

        if (!"RAZORPAY".equalsIgnoreCase(request.getPaymentGateway())) {
            throw new IllegalStateException("Unsupported payment gateway");
        }
        if (order.getRazorpayOrderId() == null
                || !order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new IllegalStateException("Payment does not match this order");
        }

        // CHANGED: the signature is now checked BEFORE the PAID/CANCELLED checks below,
        // so we only
        // ever talk about "already paid" to a caller who can prove the payment is
        // genuine.
        boolean isReallyPaid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!isReallyPaid) {
            // No FAILED row is stored: payment_details allows ONE row per order (unique
            // order_id)
            // and unique transaction_id, so a stored FAILED attempt would break a later
            // success.
            log.warn("Signature verification FAILED for order {} (razorpay order {})",
                    order.getOrderId(), request.getRazorpayOrderId());
            throw new IllegalStateException("Payment verification failed. Please try again or contact support.");
        }

        // CHANGED: the webhook may already have processed this very payment. That is a
        // success,
        // not an error - otherwise the customer is charged and then shown a failure
        // message.
        boolean samePayment = request.getRazorpayPaymentId() != null
                && request.getRazorpayPaymentId().equals(order.getRazorpayPaymentId());

        if (samePayment && order.getPaymentStatus() == PaymentStatus.PAID) {
            PaymentDetail existing = paymentRepository.findByOrder_OrderId(order.getOrderId()).orElse(null);
            return buildResponse(order, existing != null ? existing.getTransactionId()
                    : request.getRazorpayPaymentId(), existing != null ? existing.getStatus() : "COMPLETED");
        }
        if (samePayment && order.getPaymentStatus() == PaymentStatus.REFUND_PENDING) {
            throw new IllegalStateException(
                    "Your payment was received after this order expired or was cancelled. "
                            + "It will be refunded to your original payment method.");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            // A different payment id for an order that is already paid: do not mark
            // anything twice.
            log.warn("Order {} already PAID, but a second payment {} was verified. Check Razorpay for a refund.",
                    order.getOrderId(), request.getRazorpayPaymentId());
            throw new IllegalStateException("This order has already been paid for");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            // Money was taken for a cancelled order. The webhook marks it REFUND_PENDING.
            log.warn("Payment {} verified for CANCELLED order {}. Refund needed.",
                    request.getRazorpayPaymentId(), order.getOrderId());
            throw new IllegalStateException(
                    "This order was cancelled, so it can no longer be paid. "
                            + "If money was deducted, it will be refunded to your original payment method.");
        }

        PaymentDetail payment = recordCompletedPayment(order, request.getRazorpayPaymentId(),
                request.getPaymentMethod());
        markPaid(order, request.getRazorpayPaymentId());
        orderRepository.save(order);

        return buildResponse(order, payment.getTransactionId(), payment.getStatus());
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

        long expected = toPaise(order.getTotalAmount());
        if (expected != amountPaise) {
            log.error("Webhook: amount mismatch on order {} (expected {} paise, got {}). NOT marking paid.",
                    order.getOrderId(), expected, amountPaise);
            return;
        }

        recordCompletedPayment(order, razorpayPaymentId, method == null ? "ONLINE" : method.toUpperCase());

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            // Money was captured for an order we already cancelled (stock restored).
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            log.warn(
                    "Webhook: payment {} captured for CANCELLED order {}. Marked REFUND_PENDING - refund it in Razorpay.",
                    razorpayPaymentId, order.getOrderId());
        } else {
            markPaid(order, razorpayPaymentId);
            log.info("Webhook: order {} marked PAID via payment {}", order.getOrderId(), razorpayPaymentId);
        }
        orderRepository.save(order);
    }

    /**
     * Shared by the browser and webhook paths: PAID, PENDING -> CONFIRMED, email
     * after commit.
     */
    private void markPaid(Order order, String razorpayPaymentId) {
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setRazorpayPaymentId(razorpayPaymentId);
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
        }
        events.publishEvent(new OrderEmailEvent(order.getOrderId(), OrderEmailEvent.Type.CONFIRMATION));
        removePurchasedLinesFromCart(order);
    }

    // One payment row per order: update it if it exists, otherwise create it.
    private PaymentDetail recordCompletedPayment(Order order, String transactionId, String method) {
        PaymentDetail payment = paymentRepository.findByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> PaymentDetail.builder()
                        .order(order)
                        .paymentGateway("RAZORPAY")
                        .build());

        payment.setTransactionId(transactionId);
        payment.setPaymentMethod(method == null || method.isBlank() ? "ONLINE" : method);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus("COMPLETED");
        return paymentRepository.save(payment);
    }

    // NEW: builds the response in one place for the normal path and the "webhook
    // got there first" path.
    private PaymentResponse buildResponse(Order order, String transactionId, String status) {
        List<ProductSummary> items = order.getOrderItems().stream()
                .map(item -> ProductSummary.builder()
                        .name(item.getProduct() != null ? item.getProduct().getName() : "Product")
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .totalAmount(order.getTotalAmount())
                .status(status)
                .items(items)
                .build();
    }

    // NEW: the one place rupees become paise (BigDecimal, no floating-point
    // surprises).
    private long toPaise(Double rupees) {
        return BigDecimal.valueOf(rupees)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
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

    private void removePurchasedLinesFromCart(Order order) {
        if (order.getUser() == null || order.getOrderItems() == null)
            return;

        cartRepository.findByUser_UserId(order.getUser().getUserId()).ifPresent(cart -> {
            if (cart.getItems() == null || cart.getItems().isEmpty())
                return;

            cart.getItems().removeIf(cartItem -> order.getOrderItems().stream()
                    .anyMatch(orderItem -> orderItem.getProduct() != null
                            && orderItem.getProduct().getProductId().equals(cartItem.getProduct().getProductId())
                            && Objects.equals(
                                    orderItem.getVariant() != null ? orderItem.getVariant().getVariantId() : null,
                                    cartItem.getVariant() != null ? cartItem.getVariant().getVariantId() : null)));

            double total = cart.getItems().stream()
                    .mapToDouble(i -> i.getPrice() * i.getQuantity())
                    .sum();
            cart.setTotalAmount(Math.round(total * 100.0) / 100.0);
            cartRepository.save(cart);
        });
    }
}