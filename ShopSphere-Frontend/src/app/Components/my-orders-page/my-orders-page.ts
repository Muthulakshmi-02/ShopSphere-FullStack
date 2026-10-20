import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../Services/order-service';
import { OrderResponse, PaymentRequest } from '../../Models/Order.model';
import { productImageUrl } from '../../shared/api.config';
import { OrderTimeline } from '../../shared/order-timeline/order-timeline';
import { ToastService } from '../../shared/toast/toast-service';
import { ConfirmService } from '../../shared/confirm-modal/confirm-service';

// Razorpay's checkout script is loaded globally (same as on the checkout page).
declare const Razorpay: any;

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, OrderTimeline, RouterLink],
  templateUrl: './my-orders-page.html',
  styleUrls: ['./my-orders-page.css']
})
export class MyOrdersPage implements OnInit {
  private orderService = inject(OrderService);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmService);
  protected readonly productImageUrl = productImageUrl;

  orders = signal<OrderResponse[]>([]);
  loading = signal(true);
  loadError = signal(false);
  cancellingOrderId = signal<number | null>(null);
  payingOrderId = signal<number | null>(null);

  // Client-side pagination: one customer's own history is a bounded list.
  readonly pageSize = 5;
  currentPage = signal(1);

  totalPages = computed(() => Math.max(1, Math.ceil(this.orders().length / this.pageSize)));
  pageNumbers = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i + 1));
  pagedOrders = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.orders().slice(start, start + this.pageSize);
  });

  // Matches the backend's own eligibility check.
  private readonly CANCELLABLE_STATUSES = ['PENDING', 'CONFIRMED'];

  ngOnInit() {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.orderService.getUserOrderHistory().subscribe({
      next: (res) => {
        this.orders.set(res.data || []);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load orders', err);
        this.loading.set(false);
        // Used to show "You haven't placed any orders yet" when the server simply failed.
        this.loadError.set(true);
      }
    });
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) return;
    this.currentPage.set(page);
  }

  canCancel(order: OrderResponse): boolean {
    return this.CANCELLABLE_STATUSES.includes(order.orderStatus);
  }

  // A card order the customer started but has not paid for yet (it is cancelled automatically
  // after 30 minutes). Before, they could not return to the payment from here.
  canPay(order: OrderResponse): boolean {
    return order.orderStatus === 'PENDING'
      && order.paymentStatus === 'PENDING'
      && !!order.paymentMethod
      && order.paymentMethod !== 'COD';
  }

  async cancelOrder(order: OrderResponse): Promise<void> {
    const wasPaid = order.paymentStatus === 'PAID';
    const confirmed = await this.confirmService.ask({
      title: `Cancel order #${order.orderId}?`,
      message: wasPaid
        ? 'This will cancel your order. Your payment will be refunded to your original payment method. This can\'t be undone.'
        : 'This will cancel your order. This can\'t be undone.',
      confirmLabel: 'Cancel order',
      danger: true,
    });
    if (!confirmed) return;

    this.cancellingOrderId.set(order.orderId);
    this.orderService.cancelOrder(order.orderId).subscribe({
      next: (res) => {
        this.cancellingOrderId.set(null);
        // Use the server's answer: a paid order also moves to REFUND_PENDING.
        this.orders.update(list =>
          list.map(o => (o.orderId === order.orderId ? { ...o, ...(res?.data ?? { orderStatus: 'CANCELLED' }) } : o))
        );
        this.toastService.success('Order cancelled.');
      },
      error: (err) => {
        this.cancellingOrderId.set(null);
        this.toastService.error(err.error?.message || 'Could not cancel this order.');
      },
    });
  }

  // Re-opens Razorpay for an order whose payment was started but not completed.
  payNow(order: OrderResponse): void {
    this.payingOrderId.set(order.orderId);

    this.orderService.createRazorpayOrder(order.orderId).subscribe({
      next: (res) => {
        const rp = res?.data || res;
        if (!rp || !rp.keyId) {
          this.payingOrderId.set(null);
          this.toastService.error('Payment could not be started. Please try again.');
          return;
        }

        const options = {
          key: rp.keyId,
          amount: rp.amountInPaise,
          currency: rp.currency || 'INR',
          name: 'ShopSphere',
          description: `Order #${order.orderId}`,
          order_id: rp.razorpayOrderId,
          theme: { color: '#FF3D71' },
          handler: (response: any) => this.verifyPayment(order, response),
          modal: { ondismiss: () => this.payingOrderId.set(null) },
        };

        const checkout = new Razorpay(options);
        checkout.on('payment.failed', (response: any) => {
          this.payingOrderId.set(null);
          this.toastService.error(response?.error?.description || 'Payment failed. Please try again.');
        });
        checkout.open();
      },
      error: (err) => {
        this.payingOrderId.set(null);
        this.toastService.error(err.error?.message || 'Could not start payment. Please try again.');
        this.loadOrders();   // the order may have been cancelled in the meantime
      },
    });
  }

  private verifyPayment(order: OrderResponse, razorpayResponse: any): void {
    const request: PaymentRequest = {
      orderId: order.orderId,
      transactionId: razorpayResponse.razorpay_payment_id,
      paymentMethod: order.paymentMethod || 'CREDIT_CARD',
      paymentGateway: 'RAZORPAY',
      status: 'SUCCESS',
      razorpayOrderId: razorpayResponse.razorpay_order_id,
      razorpayPaymentId: razorpayResponse.razorpay_payment_id,
      razorpaySignature: razorpayResponse.razorpay_signature,
    };

    this.orderService.verifyPayment(request).subscribe({
      next: () => {
        this.payingOrderId.set(null);
        this.orders.update(list =>
          list.map(o => (o.orderId === order.orderId
            ? { ...o, paymentStatus: 'PAID', orderStatus: 'CONFIRMED' }
            : o))
        );
        this.toastService.success('Payment received. Thank you!');
      },
      error: (err) => {
        this.payingOrderId.set(null);
        this.toastService.error(
          err.error?.message || 'We could not confirm your payment. If money was deducted, contact support with your order number.'
        );
      },
    });
  }
}