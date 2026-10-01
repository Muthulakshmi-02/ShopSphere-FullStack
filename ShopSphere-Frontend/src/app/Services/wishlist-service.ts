import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { ProductResponse } from '../Models/Product.model';

export interface WishlistItemResponse {
  wishlistItemId: number;
  product: ProductResponse;
  addedAt: string;
}

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/wishlist';

  // Product IDs currently wishlisted, kept in sync so any component (product
  // cards, header badge, etc.) can check membership instantly without an
  // extra HTTP call per card.
  wishlistedIds = signal<Set<number>>(new Set());

  loadWishlistIds(): Observable<ApiResponse<number[]>> {
    return this.http.get<ApiResponse<number[]>>(`${this.API_URL}/product-ids`).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.wishlistedIds.set(new Set(res.data));
        }
      })
    );
  }

  getMyWishlist(): Observable<ApiResponse<WishlistItemResponse[]>> {
    return this.http.get<ApiResponse<WishlistItemResponse[]>>(this.API_URL);
  }

  toggle(productId: number): Observable<ApiResponse<boolean>> {
    return this.http.post<ApiResponse<boolean>>(`${this.API_URL}/${productId}/toggle`, {}).pipe(
      tap(res => {
        if (!res.success) return;
        const current = new Set(this.wishlistedIds());
        if (res.data) {
          current.add(productId);
        } else {
          current.delete(productId);
        }
        this.wishlistedIds.set(current);
      })
    );
  }

  isWishlisted(productId: number): boolean {
    return this.wishlistedIds().has(productId);
  }

  clear(): void {
    this.wishlistedIds.set(new Set());
  }
}
