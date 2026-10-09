package com.example.MyProject.Repository;

import com.example.MyProject.Models.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUser_UserIdOrderByAddedAtDesc(Long userId);

    // Loads each item's product, category and variants in ONE query. The plain version above
    // triggered a separate query for every product, category and variant list (N+1).
    @Query("select distinct w from WishlistItem w " +
           "join fetch w.product p " +
           "left join fetch p.category " +
           "left join fetch p.variants " +
           "where w.user.userId = :userId " +
           "order by w.addedAt desc")
    List<WishlistItem> findAllWithProductByUserId(@Param("userId") Long userId);

    // Only the ids. Called on every page load to light up the hearts, so it must be cheap.
    @Query("select w.product.productId from WishlistItem w where w.user.userId = :userId")
    Set<Long> findProductIdsByUserId(@Param("userId") Long userId);

    Optional<WishlistItem> findByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    boolean existsByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    long countByUser_UserId(Long userId);

    void deleteByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    // Used when a product is deleted: removes it from everyone's wishlist.
    void deleteByProduct_ProductId(Long productId);
}