export interface ReviewResponse {
  reviewId: number;
  reviewerName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ReviewRequest {
  rating: number;
  comment: string | null;
}

export interface ReviewEligibilityResponse {
  hasPurchased: boolean;
  alreadyReviewed: boolean;
  canReview: boolean;
}

export interface AdminReviewResponse {
  reviewId: number;
  productId: number;
  productName: string;
  productImageUrl?: string | null;
  reviewerName: string;
  reviewerEmail: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}
