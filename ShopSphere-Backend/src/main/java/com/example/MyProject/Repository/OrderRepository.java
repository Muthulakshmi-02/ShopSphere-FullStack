package com.example.MyProject.Repository;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser_UserIdOrderByOrderDateDesc(Long userId);

    List<Order> findAllByOrderByOrderDateDesc();

    // Real "latest 30" (SQL LIMIT). Lazy relations are read inside the @Transactional service.
    List<Order> findTop30ByOrderByOrderDateDesc();

    // Preferred for the live ticker: only orders that really happened.
    List<Order> findTop30ByOrderStatusInOrderByOrderDateDesc(Collection<OrderStatus> statuses);

    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    List<Order> findByPaymentStatusAndOrderStatusNotAndOrderDateAfter(
            PaymentStatus paymentStatus,
            OrderStatus excludedOrderStatus,
            LocalDateTime after);

    // Used by OrderService.cancelExpiredUnpaidCardOrders()
    List<Order> findByOrderStatusAndPaymentStatusAndCreatedAtBefore(
            OrderStatus orderStatus, PaymentStatus paymentStatus, LocalDateTime before);

    // Row-locked lookups so the browser callback and the webhook can never both
    // process the same payment at the same time.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.razorpayOrderId = :rzpOrderId")
    Optional<Order> findByRazorpayOrderIdForUpdate(@Param("rzpOrderId") String rzpOrderId);

    
}