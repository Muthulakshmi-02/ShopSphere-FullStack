package com.example.MyProject.Repository;

import com.example.MyProject.Models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    long countByProduct_ProductId(Long productId);

    long countByVariant_VariantId(Long variantId);

    // Powers the "only verified buyers can review" rule - true if this user
    // has any order item at all for this product, regardless of variant.
    boolean existsByOrder_User_UserIdAndProduct_ProductId(Long userId, Long productId);
}
