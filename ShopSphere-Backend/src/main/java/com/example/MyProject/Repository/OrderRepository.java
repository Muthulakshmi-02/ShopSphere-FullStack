package com.example.MyProject.Repository;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser_UserIdOrderByOrderDateDesc(Long userId);

    List<Order> findAllByOrderByOrderDateDesc();

    // Real "latest 30" (derived query => SQL LIMIT 30). The previous @Query version
    // ignored the method name and loaded EVERY order with its items and products
    // on every call of a public endpoint. Lazy relations are read inside the
    // @Transactional service method.
    List<Order> findTop30ByOrderByOrderDateDesc();

    // Preferred for the live ticker: only orders that really happened
    // (not cancelled, not unpaid card orders waiting for payment).
    List<Order> findTop30ByOrderStatusInOrderByOrderDateDesc(Collection<OrderStatus> statuses);

    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    // Used by admin analytics: only PAID orders count as revenue. Cancelling a PAID
    // order now moves it to REFUND_PENDING, so it drops out automatically; the
    // CANCELLED exclusion is kept as an extra guard.
    List<Order> findByPaymentStatusAndOrderStatusNotAndOrderDateAfter(
            PaymentStatus paymentStatus,
            OrderStatus excludedOrderStatus,
            LocalDateTime after);

    // Used by OrderService.cancelExpiredUnpaidCardOrders()
    List<Order> findByOrderStatusAndPaymentStatusAndCreatedAtBefore(
            OrderStatus orderStatus, PaymentStatus paymentStatus, LocalDateTime before);
}