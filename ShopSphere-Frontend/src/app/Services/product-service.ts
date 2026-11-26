import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { ProductResponse, ProductRequest, RestockRequest } from '../Models/Product.model';
import { API_BASE_URL } from '../shared/api.config';

// Generic Spring Data-style Page response
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;        // current page index (0-based)
}

export interface ProductStats {
  total: number;
  inStock: number;
  lowStock: number;
  outOfStock: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);

  private readonly API_URL = `${API_BASE_URL}/api/products`;
  private readonly ADMIN_URL = `${API_BASE_URL}/api/admin/products`;

  // ── In-memory cache for product listing calls ──
  // Key: serialised query params → Value: cached API response.
  // Avoids re-fetching the same page when the user navigates back.
  private listCache = new Map<string, ApiResponse<Page<ProductResponse>>>();
  // Single-product cache (by ID).
  private detailCache = new Map<number, ApiResponse<ProductResponse>>();

  /**
   * Fetches products using Backend's Unified Filter Logic.
   * Results are cached so navigating back skips a redundant HTTP call.
   */
  getFilteredProducts(
    keyword: string = '',
    categoryId: number | null = null,
    minPrice: number = 0,
    maxPrice: number = 1000000,
    page: number = 0,
    size: number = 10,
    sort: string = 'price,asc',
    stockFilter: string = 'ALL'
  ): Observable<ApiResponse<Page<ProductResponse>>> {

    let params = new HttpParams()
      .set('minPrice', minPrice.toString())
      .set('maxPrice', maxPrice.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (stockFilter && stockFilter !== 'ALL') {
      params = params.set('stockFilter', stockFilter);
    }

    const trimmed = keyword?.trim();
    if (trimmed) {
      params = params.set('keyword', trimmed);
    }

    if (categoryId != null) {
      params = params.set('categoryId', categoryId.toString());
    }

    const cacheKey = params.toString();
    const cached = this.listCache.get(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http.get<ApiResponse<Page<ProductResponse>>>(this.API_URL, { params }).pipe(
      tap(res => this.listCache.set(cacheKey, res))
    );
  }

  /**
   * Fetches a single product by ID (cached).
   */
  getProductById(productId: number): Observable<ApiResponse<ProductResponse>> {
    const cached = this.detailCache.get(productId);
    if (cached) {
      return of(cached);
    }

    return this.http.get<ApiResponse<ProductResponse>>(`${this.API_URL}/${productId}`).pipe(
      tap(res => this.detailCache.set(productId, res))
    );
  }

  // ── Admin Methods (Require JWT) ──
  // Write operations clear the cache so the next read picks up fresh data.

  createProduct(product: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.post<ApiResponse<ProductResponse>>(this.API_URL, product).pipe(
      tap(() => this.clearCache())
    );
  }

  updateProduct(id: number, product: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.put<ApiResponse<ProductResponse>>(`${this.API_URL}/${id}`, product).pipe(
      tap(() => this.clearCache())
    );
  }

  deleteProduct(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`).pipe(
      tap(() => this.clearCache())
    );
  }

  /**
   * Uploads an actual image file (admin only).
   */
  uploadImage(file: File): Observable<ApiResponse<{ imageUrl: string }>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<{ imageUrl: string }>>(`${this.ADMIN_URL}/upload-image`, formData);
  }

  /** Catalog-wide stock counts for the admin Products tab. */
  getStats(): Observable<ApiResponse<ProductStats>> {
    return this.http.get<ApiResponse<ProductStats>>(`${this.ADMIN_URL}/stats`);
  }

  /** Quick restock product inventory (admin only). */
  restockProduct(productId: number, request: RestockRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.patch<ApiResponse<ProductResponse>>(`${this.API_URL}/${productId}/restock`, request).pipe(
      tap(() => this.clearCache())
    );
  }

  /** Clears all cached data — called after any admin write. */
  clearCache(): void {
    this.listCache.clear();
    this.detailCache.clear();
  }
}
