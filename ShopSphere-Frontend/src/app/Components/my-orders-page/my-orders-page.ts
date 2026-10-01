

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../Services/order-service';
import { OrderResponse } from '../../Models/Order.model';
import { productImageUrl } from '../../shared/api.config';
import { OrderTimeline } from '../../shared/order-timeline/order-timeline';
import { ToastService } from '../../shared/toast/toast-service';
import { ConfirmService } from '../../shared/confirm-modal/confirm-service';

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
  cancellingOrderId = signal<number | null>(null);

  // Pagination - client-side, since a single customer's own order history
  // is a bounded, personal-scale list (unlike the product catalog, which
  // paginates server-side). If this ever needs to handle very large
  // histories, swap this for a real paginated backend call, same pattern
  // as ProductService.getFilteredProducts().
  readonly pageSize = 5;
  currentPage = signal(1);

  totalPages = computed(() => Math.max(1, Math.ceil(this.orders().length / this.pageSize)));

  pageNumbers = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i + 1));

  pagedOrders = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.orders().slice(start, start + this.pageSize);
  });

  // Only orders still in these states can be self-service cancelled -
  // matches the backend's own eligibility check.
  private readonly CANCELLABLE_STATUSES = ['PENDING', 'CONFIRMED'];

  ngOnInit() {
    this.orderService.getUserOrderHistory().subscribe({
      next: (res) => {
        // Your ApiResponse has a 'data' field containing the list
        this.orders.set(res.data || []);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load orders', err);
        this.loading.set(false);
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

  async cancelOrder(order: OrderResponse): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: `Cancel order #${order.orderId}?`,
      message: 'This will cancel your order and restock the items. This can\'t be undone.',
      confirmLabel: 'Cancel order',
      danger: true,
    });
    if (!confirmed) return;

    this.cancellingOrderId.set(order.orderId);
    this.orderService.cancelOrder(order.orderId).subscribe({
      next: (res) => {
        this.cancellingOrderId.set(null);
        this.orders.update(list =>
          list.map(o => (o.orderId === order.orderId ? { ...o, orderStatus: 'CANCELLED' } : o))
        );
        this.toastService.success('Order cancelled.');
      },
      error: (err) => {
        this.cancellingOrderId.set(null);
        this.toastService.error(err.error?.message || 'Could not cancel this order.');
      },
    });
  }
}