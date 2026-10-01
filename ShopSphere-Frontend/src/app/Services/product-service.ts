// import { Injectable } from '@angular/core';
// import { inject } from '@angular/core';
// import { HttpClient, HttpParams } from '@angular/common/http';
// import { Observable } from 'rxjs';
// import { ApiResponse } from '../Models/User.model'; // Assuming this is your standard wrapper
// import { ProductResponse, ProductRequest } from '../Models/Product.model';

// @Injectable({ providedIn: 'root' })
// export class ProductService {
//   private http = inject(HttpClient);
//   private readonly API_URL = '/api/products';

//   /**
//    * Fetches products using your Backend's Unified Filter Logic
//    * Maps to: @GetMapping in ProductController
//    */
//   getFilteredProducts(
//     keyword: string = '',
//     categoryId: number | null = null,
//     minPrice: number = 0,
//     maxPrice: number = 1000000,
//     page: number = 0,
//     size: number = 10,
//     sort: string = 'price,asc'
//   ): Observable<ApiResponse<any>> {

//     // Build parameters exactly as your @RequestParam expects them
//     let params = new HttpParams()
//       .set('minPrice', minPrice.toString())
//       .set('maxPrice', maxPrice.toString())
//       .set('page', page.toString())
//       .set('size', size.toString())
//       .set('sort', sort);

//     if (keyword) params = params.set('keyword', keyword);
//     if (categoryId) params = params.set('categoryId', categoryId.toString());

//     return this.http.get<ApiResponse<any>>(this.API_URL, { params });
//   }

//   /**
//    * Fetches a single product by ID
//    * Maps to: @GetMapping("/{productId}")
//    */
//   getProductById(productId: number): Observable<ApiResponse<ProductResponse>> {
//     return this.http.get<ApiResponse<ProductResponse>>(`${this.API_URL}/${productId}`);
//   }

//   // Admin Methods (Require JWT)
//   createProduct(product: ProductRequest): Observable<ApiResponse<ProductResponse>> {
//     return this.http.post<ApiResponse<ProductResponse>>(this.API_URL, product);
//   }

//   updateProduct(id: number, product: ProductRequest): Observable<ApiResponse<ProductResponse>> {
//     return this.http.put<ApiResponse<ProductResponse>>(`${this.API_URL}/${id}`, product);
//   }

//   deleteProduct(id: number): Observable<ApiResponse<void>> {
//     return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
//   }
// }

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { ProductResponse, ProductRequest } from '../Models/Product.model';

// Generic Spring Data-style Page response
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;        // current page index (0-based)
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8080/api/products';

  /**
   * Fetches products using Backend's Unified Filter Logic
   * Maps to: @GetMapping in ProductController
   */
  getFilteredProducts(
    keyword: string = '',
    categoryId: number | null = null,
    minPrice: number = 0,
    maxPrice: number = 1000000,
    page: number = 0,
    size: number = 10,
    sort: string = 'price,asc'
  ): Observable<ApiResponse<Page<ProductResponse>>> {

    let params = new HttpParams()
      .set('minPrice', minPrice.toString())
      .set('maxPrice', maxPrice.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    const trimmed = keyword?.trim();
    if (trimmed) {
      params = params.set('keyword', trimmed);
    }

    if (categoryId != null) {
      params = params.set('categoryId', categoryId.toString());
    }

    return this.http.get<ApiResponse<Page<ProductResponse>>>(this.API_URL, { params });
  }

  /**
   * Fetches a single product by ID
   * Maps to: @GetMapping("/{productId}")
   */
  getProductById(productId: number): Observable<ApiResponse<ProductResponse>> {
    return this.http.get<ApiResponse<ProductResponse>>(`${this.API_URL}/${productId}`);
  }

  // Admin Methods (Require JWT)
  createProduct(product: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.post<ApiResponse<ProductResponse>>(this.API_URL, product);
  }

  updateProduct(id: number, product: ProductRequest): Observable<ApiResponse<ProductResponse>> {
    return this.http.put<ApiResponse<ProductResponse>>(`${this.API_URL}/${id}`, product);
  }

  deleteProduct(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
  }

  /**
   * Uploads an actual image file (admin only). Returns the generated
   * filename to store as the product's imageUrl - the backend serves it
   * back out at /images/<filename> alongside the original seed images.
   */
  uploadImage(file: File): Observable<ApiResponse<{ imageUrl: string }>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<{ imageUrl: string }>>(
      'http://localhost:8080/api/admin/products/upload-image',
      formData
    );
  }

}
