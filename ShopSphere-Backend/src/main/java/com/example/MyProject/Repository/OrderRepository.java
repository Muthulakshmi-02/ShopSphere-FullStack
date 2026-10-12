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

    // One query that brings each order's lines AND their products (the plain finders above
    // ran extra queries per order and per line: N+1).
    @Query("select distinct o from Order o " +
           "left join fetch o.orderItems i " +
           "left join fetch i.product " +
           "where o.user.userId = :userId " +
           "order by o.orderDate desc")
    List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("select distinct o from Order o " +
           "join fetch o.user " +
           "left join fetch o.orderItems i " +
           "left join fetch i.product " +
           "order by o.orderDate desc")
    List<Order> findAllWithItems();

    // Real "latest 30" (SQL LIMIT). Lazy relations are read inside the @Transactional service.
    List<Order> findTop30ByOrderByOrderDateDesc();

    // For the live ticker: only orders that really happened.
    List<Order> findTop30ByOrderStatusInOrderByOrderDateDesc(Collection<OrderStatus> statuses);

    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    List<Order> findByPaymentStatusAndOrderStatusNotAndOrderDateAfter(
            PaymentStatus paymentStatus,
            OrderStatus excludedOrderStatus,
            LocalDateTime after);

    // Analytics: just (orderDate, totalAmount) of paid, non-cancelled orders. No entities,
    // no order lines, no products are loaded.
    @Query("select o.orderDate, o.totalAmount from Order o " +
           "where o.paymentStatus = :paid and o.orderStatus <> :cancelled and o.orderDate >= :since")
    List<Object[]> findPaidOrderPoints(@Param("paid") PaymentStatus paid,
                                       @Param("cancelled") OrderStatus cancelled,
                                       @Param("since") LocalDateTime since);

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