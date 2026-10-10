package com.example.MyProject.Repository;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    long countByProduct_ProductId(Long productId);

    long countByVariant_VariantId(Long variantId);

    // ANY order line at all, including cancelled and unpaid orders. No longer used for reviews.
    boolean existsByOrder_User_UserIdAndProduct_ProductId(Long userId, Long productId);

    // The "verified buyer" rule for reviews: the user has an order for this product that
    // has actually reached the given status (DELIVERED). A cancelled or still-pending order
    // does not count.
    boolean existsByOrder_User_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
            Long userId, Long productId, OrderStatus orderStatus);
}