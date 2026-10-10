package com.example.MyProject.Repository;

import com.example.MyProject.Models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Called when a product is deleted: removes it from everyone's cart.
    @Modifying
    @Query("delete from CartItem c where c.product.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    // Called when an admin removes a variant. Now ONE delete statement (like the method above)
    // instead of Spring Data loading every matching row and deleting them one by one.
    // Cart totals are recalculated the next time the cart is read.
    @Modifying
    @Query("delete from CartItem c where c.variant.variantId = :variantId")
    void deleteByVariant_VariantId(@Param("variantId") Long variantId);
}