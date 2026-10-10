package com.example.MyProject.Controller;

import com.example.MyProject.Review.dto.ReviewEligibilityResponse;
import com.example.MyProject.Review.dto.ReviewRequest;
import com.example.MyProject.Review.dto.ReviewResponse;
import com.example.MyProject.Services.ReviewService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @CrossOrigin removed: CORS is configured once in SecurityConfig.
@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Public: anyone browsing can read reviews, logged in or not.
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    // Lets the frontend show the right state (write a review / already reviewed / unlock later).
    @GetMapping("/eligibility")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ReviewEligibilityResponse>> checkEligibility(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.checkEligibility(productId));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.submitReview(productId, request));
    }

    // Admin moderation. SecurityConfig already limits DELETE under /api/products/** to ADMIN.
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long productId, @PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.deleteReview(productId, reviewId));
    }
}