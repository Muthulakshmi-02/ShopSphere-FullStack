import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { API_BASE_URL } from '../shared/api.config';

export interface DailyRevenuePoint {
  date: string;
  revenue: number;
  orderCount: number;
}

export interface TopProductPoint {
  productId: number;
  productName: string;
  unitsSold: number;
  revenue: number;
}

export interface AnalyticsResponse {
  rangeDays: number;
  totalRevenue: number;
  totalOrders: number;
  averageOrderValue: number;
  revenueByDay: DailyRevenuePoint[];
  topProducts: TopProductPoint[];
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private http = inject(HttpClient);

  // CHANGED: was 'http://localhost:8080/api/admin/analytics'. It follows API_BASE_URL now.
  private readonly API_URL = `${API_BASE_URL}/api/admin/analytics`;

  getAnalytics(days: number = 30): Observable<ApiResponse<AnalyticsResponse>> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<ApiResponse<AnalyticsResponse>>(this.API_URL, { params });
  }
}