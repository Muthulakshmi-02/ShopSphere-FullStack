import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { CategoryRequest, CategoryResponse } from '../Models/Category.model';
import { API_BASE_URL } from '../shared/api.config';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);
  private readonly API_URL = `${API_BASE_URL}/api/categories`;

  // ── In-memory cache for the category list ──
  private cachedCategories: ApiResponse<CategoryResponse[]> | null = null;

  getAllCategories(): Observable<ApiResponse<CategoryResponse[]>> {
    if (this.cachedCategories) {
      return of(this.cachedCategories);
    }
    return this.http.get<ApiResponse<CategoryResponse[]>>(this.API_URL).pipe(
      tap(res => this.cachedCategories = res)
    );
  }

  createCategory(data: CategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.post<ApiResponse<CategoryResponse>>(this.API_URL, data).pipe(
      tap(() => this.cachedCategories = null)
    );
  }

  updateCategory(categoryId: number, data: CategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.put<ApiResponse<CategoryResponse>>(`${this.API_URL}/${categoryId}`, data).pipe(
      tap(() => this.cachedCategories = null)
    );
  }

  deleteCategory(categoryId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${categoryId}`).pipe(
      tap(() => this.cachedCategories = null)
    );
  }
}
