package com.example.MyProject.Services;

import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.Review;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.OrderItemRepository;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.Repository.ReviewRepository;
import com.example.MyProject.Repository.UserRepository;
import com.example.MyProject.Review.dto.ReviewEligibilityResponse;
import com.example.MyProject.Review.dto.ReviewRequest;
import com.example.MyProject.Review.dto.ReviewResponse;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Real customer reviews - gated behind having actually purchased the
 * product, which is what replaces the admin-typed rating/reviewCount
 * numbers on Product with genuine feedback. Every time a review is
 * submitted, the product's cached rating/reviewCount are recomputed from
 * the real review data, so all the existing star-rating display code
 * across product cards / product detail keeps working unchanged - it just
 * shows real numbers now instead of admin-entered ones.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ApiResponse<List<ReviewResponse>> getProductReviews(Long productId) {
        List<ReviewResponse> reviews = reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.<List<ReviewResponse>>builder().success(true).data(reviews).build();
    }

    public ApiResponse<ReviewEligibilityResponse> checkEligibility(Long productId) {
        User user = getAuthenticatedUser();
        boolean hasPurchased = orderItemRepository.existsByOrder_User_UserIdAndProduct_ProductId(
                user.getUserId(), productId);
        boolean alreadyReviewed = reviewRepository.existsByUser_UserIdAndProduct_ProductId(
                user.getUserId(), productId);

        ReviewEligibilityResponse eligibility = ReviewEligibilityResponse.builder()
                .hasPurchased(hasPurchased)
                .alreadyReviewed(alreadyReviewed)
                .canReview(hasPurchased && !alreadyReviewed)
                .build();

        return ApiResponse.<ReviewEligibilityResponse>builder().success(true).data(eligibility).build();
    }

    @Transactional
    public ApiResponse<ReviewResponse> submitReview(Long productId, ReviewRequest request) {
        User user = getAuthenticatedUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean hasPurchased = orderItemRepository.existsByOrder_User_UserIdAndProduct_ProductId(
                user.getUserId(), productId);
        if (!hasPurchased) {
            throw new CartBusinessException("Only customers who've purchased this product can review it.");
        }

        boolean alreadyReviewed = reviewRepository.existsByUser_UserIdAndProduct_ProductId(
                user.getUserId(), productId);
        if (alreadyReviewed) {
            throw new CartBusinessException("You've already reviewed this product.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        Review saved = reviewRepository.save(review);

        recomputeProductRating(product);

        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Review submitted")
                .data(mapToResponse(saved))
                .build();
    }

    /**
     * Recalculates a product's displayed rating/reviewCount from real
     * review data. Called after every review submission so the numbers
     * shown everywhere else in the app (product cards, detail page) stay
     * truthful without those components needing to know reviews exist.
     */
    private void recomputeProductRating(Product product) {
        Double average = reviewRepository.findAverageRatingForProduct(product.getProductId());
        long count = reviewRepository.countByProduct_ProductId(product.getProductId());

        product.setRating(average != null ? Math.round(average * 10.0) / 10.0 : null);
        product.setReviewCount((int) count);
        productRepository.save(product);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .reviewerName(review.getUser().getUserName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        String email = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
