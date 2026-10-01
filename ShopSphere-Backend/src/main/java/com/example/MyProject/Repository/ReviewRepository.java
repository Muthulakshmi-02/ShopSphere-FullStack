package com.example.MyProject.Repository;

import com.example.MyProject.Models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct_ProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    Optional<Review> findByReviewIdAndUser_UserId(Long reviewId, Long userId);

    long countByProduct_ProductId(Long productId);

    @Query("select avg(r.rating) from Review r where r.product.productId = :productId")
    Double findAverageRatingForProduct(@Param("productId") Long productId);
}
