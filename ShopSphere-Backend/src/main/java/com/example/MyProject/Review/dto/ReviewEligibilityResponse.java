package com.example.MyProject.Review.dto;

import lombok.*;

/**
 * Tells the frontend whether/why to show the "write a review" form -
 * avoids the client having to guess from a failed POST, and lets the UI
 * show a clear reason ("purchase this to review it" vs "you already
 * reviewed this") instead of just hiding the form silently.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEligibilityResponse {
    private boolean canReview;
    private boolean hasPurchased;
    private boolean alreadyReviewed;
}
