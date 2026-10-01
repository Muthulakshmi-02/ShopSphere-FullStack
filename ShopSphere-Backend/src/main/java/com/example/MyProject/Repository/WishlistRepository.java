package com.example.MyProject.Repository;

import com.example.MyProject.Models.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUser_UserIdOrderByAddedAtDesc(Long userId);

    Optional<WishlistItem> findByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    boolean existsByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    void deleteByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    // Used when a product is deleted - removes it from everyone's wishlist,
    // not just one user's.
    void deleteByProduct_ProductId(Long productId);
}
