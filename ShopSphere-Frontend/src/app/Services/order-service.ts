import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderRequest, OrderResponse, PaymentRequest } from '../Models/Order.model';
import { API_BASE_URL } from '../shared/api.config';

// withCredentials: true was removed from every call below. It only matters for COOKIE logins;
// this app authenticates with a Bearer token that the interceptor adds, so it did nothing except
// force the browser into stricter cross-origin rules.
@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private http = inject(HttpClient);
  private baseUrl = `${API_BASE_URL}/api`;
  private apiUrl = `${this.baseUrl}/orders`;
  private adminApiUrl = `${this.baseUrl}/admin/orders`;
  private paymentUrl = `${this.baseUrl}/payments`;

  /** Place a new order. Maps to: POST /api/orders/checkout */
  checkout(request: OrderRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/checkout`, request);
  }

  /** Current user's order history. Maps to: GET /api/orders/my-orders */
  getUserOrderHistory(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/my-orders`);
  }

  /**
   * Customer self-service cancellation - only allowed while the order is PENDING/CONFIRMED
   * (enforced server-side). Maps to: POST /api/orders/{orderId}/cancel
   */
  cancelOrder(orderId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${orderId}/cancel`, {});
  }

  /** Public recent-purchase feed for the live ticker (works for logged-out visitors). */
  getRecentActivity(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/recent-activity`);
  }

  /** For Admin: all orders. Maps to: GET /api/admin/orders/all */
  getAllOrders(): Observable<any> {
    return this.http.get<any>(`${this.adminApiUrl}/all`);
  }

  /**
   * Step 1 of the Razorpay flow: asks the backend to create (or reuse) the Razorpay order for
   * this internal order. Maps to: POST /api/payments/razorpay/create-order/{orderId}
   */
  createRazorpayOrder(orderId: number): Observable<any> {
    return this.http.post<any>(`${this.paymentUrl}/razorpay/create-order/${orderId}`, {});
  }

  /** Verify and save payment details. Maps to: POST /api/payments/verify */
  verifyPayment(request: PaymentRequest): Observable<any> {
    return this.http.post<any>(`${this.paymentUrl}/verify`, request);
  }

  /** Admin: update order status. Maps to: PUT /api/admin/orders/{orderId}/status?status=NEW_STATUS */
  updateOrderStatus(orderId: number, status: string): Observable<any> {
    return this.http.put<any>(
      `${this.adminApiUrl}/${orderId}/status?status=${encodeURIComponent(status)}`,
      {}
    );
  }
}