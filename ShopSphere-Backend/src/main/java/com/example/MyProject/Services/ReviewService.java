package com.example.MyProject.Services;

import com.example.MyProject.Enum.OrderStatus;
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
 * Real customer reviews, limited to verified buyers. Every change recomputes the product's
 * rating and reviewCount from the real review rows, so the numbers shown on product cards and
 * the detail page are always genuine.
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
        List<ReviewResponse> reviews = reviewRepository.findTop50ByProduct_ProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.<List<ReviewResponse>>builder().success(true).data(reviews).build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<ReviewEligibilityResponse> checkEligibility(Long productId) {
        User user = getAuthenticatedUser();

        boolean hasPurchased = hasDeliveredPurchase(user.getUserId(), productId);
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

        // Before: ANY order line counted, even a cancelled or unpaid one, so someone could place
        // an order, review instantly, then cancel. Now the order must have been DELIVERED.
        if (!hasDeliveredPurchase(user.getUserId(), productId)) {
            throw new CartBusinessException(
                    "You can review this product once an order containing it has been delivered.");
        }

        if (reviewRepository.existsByUser_UserIdAndProduct_ProductId(user.getUserId(), productId)) {
            throw new CartBusinessException("You've already reviewed this product.");
        }

        String comment = request.getComment() == null ? null : request.getComment().trim();
        if (comment != null && comment.isEmpty()) {
            comment = null;
        }

        Review saved = reviewRepository.save(Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(comment)
                .build());

        recomputeProductRating(product);

        return ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Review submitted")
                .data(mapToResponse(saved))
                .build();
    }

    /** Admin moderation: removes an abusive or fake review and recalculates the rating. */
    @Transactional
    public ApiResponse<Void> deleteReview(Long productId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // The review must belong to the product in the URL.
        if (!review.getProduct().getProductId().equals(productId)) {
            throw new ResourceNotFoundException("Review not found");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        reviewRepository.delete(review);
        reviewRepository.flush();
        recomputeProductRating(product);

        return ApiResponse.<Void>builder().success(true).message("Review deleted").build();
    }

    private boolean hasDeliveredPurchase(Long userId, Long productId) {
        return orderItemRepository.existsByOrder_User_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
                userId, productId, OrderStatus.DELIVERED);
    }

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
                .reviewerName(publicName(review.getUser().getUserName()))
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    // Usernames can be real full names ("Muthulakshmi B"); on a public page show the first
    // name and only the initial of the rest.
    private String publicName(String userName) {
        if (userName == null || userName.isBlank()) return "Customer";
        String[] parts = userName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
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