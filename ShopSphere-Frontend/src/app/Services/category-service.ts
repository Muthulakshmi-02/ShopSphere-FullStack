import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { CategoryRequest, CategoryResponse } from '../Models/Category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);
  // Ensure this matches your Spring Boot server exact address
  private readonly API_URL = 'http://localhost:8080/api/categories';

  getAllCategories(): Observable<ApiResponse<CategoryResponse[]>> {
    return this.http.get<ApiResponse<CategoryResponse[]>>(this.API_URL);
  }

  createCategory(data: CategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.post<ApiResponse<CategoryResponse>>(this.API_URL, data);
  }

  updateCategory(categoryId: number, data: CategoryRequest): Observable<ApiResponse<CategoryResponse>> {
    return this.http.put<ApiResponse<CategoryResponse>>(`${this.API_URL}/${categoryId}`, data);
  }

  deleteCategory(categoryId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${categoryId}`);
  }
}