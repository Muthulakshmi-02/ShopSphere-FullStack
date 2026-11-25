package com.example.MyProject.Review.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewResponse {
    private Long reviewId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String reviewerName;
    private String reviewerEmail;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
