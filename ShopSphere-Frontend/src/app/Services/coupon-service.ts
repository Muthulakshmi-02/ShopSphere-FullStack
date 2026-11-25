import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { CouponRequest, CouponResponse } from '../Models/Coupon.model';
import { API_BASE_URL } from '../shared/api.config';

@Injectable({ providedIn: 'root' })
export class CouponService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/api/admin/coupons`;

  getAllCoupons(): Observable<ApiResponse<CouponResponse[]>> {
    return this.http.get<ApiResponse<CouponResponse[]>>(this.baseUrl);
  }

  getCouponById(id: number): Observable<ApiResponse<CouponResponse>> {
    return this.http.get<ApiResponse<CouponResponse>>(`${this.baseUrl}/${id}`);
  }

  createCoupon(dto: CouponRequest): Observable<ApiResponse<CouponResponse>> {
    return this.http.post<ApiResponse<CouponResponse>>(this.baseUrl, dto);
  }

  updateCoupon(id: number, dto: CouponRequest): Observable<ApiResponse<CouponResponse>> {
    return this.http.put<ApiResponse<CouponResponse>>(`${this.baseUrl}/${id}`, dto);
  }

  toggleActive(id: number): Observable<ApiResponse<CouponResponse>> {
    return this.http.patch<ApiResponse<CouponResponse>>(`${this.baseUrl}/${id}/toggle`, {});
  }

  deleteCoupon(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
