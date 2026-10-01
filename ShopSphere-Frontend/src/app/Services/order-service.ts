import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
// Adjust path
import { OrderRequest, OrderResponse, PaymentRequest } from '../Models/Order.model';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api'; 
  private apiUrl = `${this.baseUrl}/orders`;
  private adminApiUrl = `${this.baseUrl}/admin/orders`;
  private paymentUrl = `${this.baseUrl}/payments`;

  /**
   * Place a new order
   * Maps to: POST /api/orders/checkout
   */
  checkout(request: OrderRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/checkout`, request, {
      withCredentials: true,
    });
  }

  /**
   * Fetch current user's order history
   * Maps to: GET /api/orders/my-orders
   */
  getUserOrderHistory(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/my-orders`, {
      withCredentials: true,
    });
  }

  /**
   * Customer self-service cancellation - only allowed while the order is
   * still PENDING/CONFIRMED (enforced server-side).
   * Maps to: POST /api/orders/{orderId}/cancel
   */
  cancelOrder(orderId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${orderId}/cancel`, {}, {
      withCredentials: true,
    });
  }

  /**
   * Real recent-purchase feed for the storefront live ticker - public
   * endpoint, no auth required, so it works for logged-out visitors too.
   */
  getRecentActivity(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/recent-activity`);
  }

  /**
   * For Admin: Fetch all orders
   * Maps to: GET /api/admin/orders/all
   */
  getAllOrders(): Observable<any> {
    return this.http.get<any>(`${this.adminApiUrl}/all`, {
      withCredentials: true,
    });
  }

  /**
   * Verify and save payment details after checkout
   * Maps to: POST /api/payments/verify
   */
  /**
   * Step 1 of the real Razorpay flow - asks the backend to create a
   * Razorpay order for this internal order, returning what's needed to
   * open the Checkout widget.
   */
  createRazorpayOrder(orderId: number): Observable<any> {
    return this.http.post<any>(`${this.paymentUrl}/razorpay/create-order/${orderId}`, {}, {
      withCredentials: true,
    });
  }

  verifyPayment(request: PaymentRequest): Observable<any> {
    return this.http.post<any>(`${this.paymentUrl}/verify`, request, {
      withCredentials: true,
    });
  }

  /**
   * Update Order Status (Admin feature)
   * Maps to: PUT /api/admin/orders/{orderId}/status?status=NEW_STATUS
   */
  updateOrderStatus(orderId: number, status: string): Observable<any> {
    return this.http.put<any>(
      `${this.adminApiUrl}/${orderId}/status?status=${status}`,
      {},
      { withCredentials: true }
    );
  }
}