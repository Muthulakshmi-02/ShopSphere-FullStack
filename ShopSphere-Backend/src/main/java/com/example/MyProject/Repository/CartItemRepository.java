package com.example.MyProject.Repository;

import com.example.MyProject.Models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Modifying
    @Query("delete from CartItem c where c.product.productId = :productId")
    void deleteByProductId(Long productId);
}
