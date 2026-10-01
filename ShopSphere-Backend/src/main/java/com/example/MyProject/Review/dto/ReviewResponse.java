package com.example.MyProject.Review.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long reviewId;
    // Reviewer's username only - never expose email or other account
    // details through a public review listing.
    private String reviewerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
