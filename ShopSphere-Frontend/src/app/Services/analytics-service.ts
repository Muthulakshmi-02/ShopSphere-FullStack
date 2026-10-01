import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';

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
  private readonly API_URL = 'http://localhost:8080/api/admin/analytics';

  getAnalytics(days: number = 30): Observable<ApiResponse<AnalyticsResponse>> {
    return this.http.get<ApiResponse<AnalyticsResponse>>(`${this.API_URL}?days=${days}`);
  }
}
