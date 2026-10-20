import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { ReviewEligibilityResponse, ReviewRequest, ReviewResponse } from '../Models/Review.model';
import { API_BASE_URL } from '../shared/api.config';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/api/products`;

  getReviews(productId: number): Observable<ApiResponse<ReviewResponse[]>> {
    return this.http.get<ApiResponse<ReviewResponse[]>>(`${this.baseUrl}/${productId}/reviews`);
  }

  checkEligibility(productId: number): Observable<ApiResponse<ReviewEligibilityResponse>> {
    return this.http.get<ApiResponse<ReviewEligibilityResponse>>(`${this.baseUrl}/${productId}/reviews/eligibility`);
  }

  submitReview(productId: number, request: ReviewRequest): Observable<ApiResponse<ReviewResponse>> {
    return this.http.post<ApiResponse<ReviewResponse>>(`${this.baseUrl}/${productId}/reviews`, request);
  }

  // Admin moderation (the backend endpoint exists; there is no button for it in the admin pages yet).
  deleteReview(productId: number, reviewId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${productId}/reviews/${reviewId}`);
  }
}