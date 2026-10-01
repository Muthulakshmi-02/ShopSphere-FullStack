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

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4500", allowCredentials = "true")
public class ReviewController {

    private final ReviewService reviewService;

    // Public - anyone browsing can read reviews, logged in or not.
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    // Lets the frontend show the right UI state (write a review / already
    // reviewed / purchase to unlock) instead of guessing from a failed POST.
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
}
