/**
 * Matches Backend: CategoryResponse.java
 */
export interface CategoryResponse {
  categoryId: number;
  name: string;
  description: string;
}

/**
 * Matches Backend: CategoryRequest.java
 */
export interface CategoryRequest {
  name: string;
  description: string;
}