package com.example.MyProject.Repository;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    long countByProduct_ProductId(Long productId);

    long countByVariant_VariantId(Long variantId);

    // ANY order line at all, including cancelled and unpaid orders. Not used for reviews.
    boolean existsByOrder_User_UserIdAndProduct_ProductId(Long userId, Long productId);

    // The "verified buyer" rule for reviews: an order for this product that reached DELIVERED.
    boolean existsByOrder_User_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
            Long userId, Long productId, OrderStatus orderStatus);

    // Analytics "best sellers": one grouped query, newest data only, top N via the Pageable.
    // Row = [productId, productName, unitsSold, revenue].
    // (Revenue here is price x quantity of the lines, before any order-level promo discount.)
    @Query("select p.productId, p.name, sum(oi.quantity), sum(oi.price * oi.quantity) " +
           "from OrderItem oi join oi.product p join oi.order o " +
           "where o.paymentStatus = :paid and o.orderStatus <> :cancelled and o.orderDate >= :since " +
           "group by p.productId, p.name " +
           "order by sum(oi.price * oi.quantity) desc")
    List<Object[]> findTopProductsByRevenue(@Param("paid") PaymentStatus paid,
                                            @Param("cancelled") OrderStatus cancelled,
                                            @Param("since") LocalDateTime since,
                                            Pageable pageable);
}