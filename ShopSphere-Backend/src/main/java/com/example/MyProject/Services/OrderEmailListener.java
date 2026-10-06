package com.example.MyProject.Services;

import com.example.MyProject.Models.Order;
import com.example.MyProject.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends order emails after the order transaction has committed, on a background
 * thread. Checkout never waits for SMTP, and a mail failure can't roll anything back.
 * Needs @EnableAsync on your application class.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEmailListener {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onOrderEmail(OrderEmailEvent event) {
        try {
            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) return;

            switch (event.type()) {
                case CONFIRMATION -> emailService.sendOrderConfirmation(order);
                case STATUS_UPDATE -> emailService.sendOrderStatusUpdate(order);
                case CANCELLED -> emailService.sendOrderCancelled(order);
            }
        } catch (Exception e) {
            log.warn("Order email '{}' for order {} failed: {}", event.type(), event.orderId(), e.getMessage());
        }
    }
}