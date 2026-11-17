package com.example.MyProject.Services;

import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Models.Order;
import com.example.MyProject.Repository.OrderRepository;
import com.example.MyProject.Repository.PaymentRepository;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Sends the money for a cancelled, paid order back to the customer through Razorpay.
 *
 * A paid order that is cancelled (by the customer, or because a payment arrived after the order
 * expired) is left at REFUND_PENDING. An admin then triggers the refund from the Orders page.
 * Full refunds only: the whole amount that was charged goes back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final ApplicationEventPublisher events;

    @Transactional
    public Order refund(Long orderId) {
        // Row lock: two admins pressing "Refund" at the same moment cannot both reach Razorpay.
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new CartBusinessException("This order has already been refunded.");
        }
        if (order.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            throw new CartBusinessException(
                    "Only a cancelled order that is waiting for a refund can be refunded.");
        }

        String paymentId = order.getRazorpayPaymentId();
        if (paymentId == null || paymentId.isBlank()) {
            throw new CartBusinessException(
                    "This order has no online payment on record, so it cannot be refunded here. "
                            + "Refund it in the Razorpay dashboard instead.");
        }

        long amountInPaise = BigDecimal.valueOf(order.getTotalAmount())
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        String refundId = null;
        try {
            refundId = razorpayService.refundPayment(paymentId, amountInPaise, "refund_order_" + order.getOrderId());
        } catch (RazorpayException e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("fully refunded") || message.contains("already been refunded")) {
                // Razorpay already sent the money back (for example the dashboard was used, or an earlier
                // click succeeded but could not be saved). Just bring our records up to date.
                log.warn("Order {}: Razorpay says payment {} was already refunded. Updating our records.",
                        order.getOrderId(), paymentId);
            } else {
                log.error("Order {}: Razorpay refused the refund of payment {}: {}",
                        order.getOrderId(), paymentId, e.getMessage(), e);
                throw new IllegalStateException(
                        "Razorpay could not process the refund right now: " + e.getMessage());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setRefundId(refundId);
        order.setRefundedAt(now);
        orderRepository.save(order);

        // Keep the payment record in step ("COMPLETED" or "REFUNDED").
        paymentRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(payment -> {
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
        });

        log.info("Order {} refunded: {} paise via payment {} (refund {}), by {}",
                order.getOrderId(), amountInPaise, paymentId, refundId, currentAdmin());

        // The customer is emailed after the transaction commits.
        events.publishEvent(new OrderEmailEvent(order.getOrderId(), OrderEmailEvent.Type.REFUNDED));
        return order;
    }

    private String currentAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "unknown" : String.valueOf(authentication.getName());
    }
}