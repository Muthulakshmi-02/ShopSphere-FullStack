/**
 * Matches Backend: CategoryResponse.java
 */
export interface CategoryResponse {
  categoryId: number;
  name: string;
  productCount?: number;
  description: string;
}

/**
 * Matches Backend: CategoryRequest.java
 */
export interface CategoryRequest {
  name: string;
  description: string;
}