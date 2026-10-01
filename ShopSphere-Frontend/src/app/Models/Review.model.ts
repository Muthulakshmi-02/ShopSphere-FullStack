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
