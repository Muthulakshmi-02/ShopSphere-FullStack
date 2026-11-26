import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Page, ProductStats } from '../../Services/product-service';
import { CategoryService } from '../../Services/category-service';
import { CouponService } from '../../Services/coupon-service';
import { CouponRequest, CouponResponse } from '../../Models/Coupon.model';
import { ReviewService } from '../../Services/review-service';
import { AdminReviewResponse } from '../../Models/Review.model';
import {
  ProductResponse,
  ProductRequest,
  ProductStatus,
  RestockRequest,
} from '../../Models/Product.model';
import {
  CategoryResponse,
  CategoryRequest,
} from '../../Models/Category.model';
import { productImageUrl } from '../../shared/api.config';
import { OrderService } from '../../Services/order-service';
import { AnalyticsService, AnalyticsResponse } from '../../Services/analytics-service';
import { OrderResponse } from '../../Models/Order.model';
import { ToastService } from '../../shared/toast/toast-service';
import { ConfirmService } from '../../shared/confirm-modal/confirm-service';
import { MatIconModule } from '@angular/material/icon';

// Which status an order may move to next. Mirrors OrderService.updateOrderStatus on the server,
// which refuses everything else (e.g. a CANCELLED order cannot become DELIVERED).
const NEXT_STATUSES: Record<string, string[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
};

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css'],
})
export class AdminDashboard implements OnInit {
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private orderService = inject(OrderService);
  private analyticsService = inject(AnalyticsService);
  private couponService = inject(CouponService);
  private reviewService = inject(ReviewService);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmService);

  protected readonly productImageUrl = productImageUrl;

  // View state
  activeTab = signal<'products' | 'categories' | 'orders' | 'analytics' | 'coupons' | 'reviews' | ''>('products');
  isModalOpen = signal(false);
  isEditMode = signal(false);
  selectedId = signal<number | null>(null);
  loading = signal(false);
  uploadingImage = signal(false);

  // --- Reviews tab state ---
  adminReviews = signal<AdminReviewResponse[]>([]);
  reviewsLoading = signal(false);
  reviewSearch = signal('');
  reviewRatingFilter = signal<number | 'ALL'>('ALL');
  deletingReviewId = signal<number | null>(null);

  filteredReviews = computed(() => {
    const q = this.reviewSearch().trim().toLowerCase();
    const star = this.reviewRatingFilter();
    return this.adminReviews().filter(r => {
      if (star !== 'ALL' && r.rating !== Number(star)) return false;
      if (!q) return true;
      return (
        (r.productName && r.productName.toLowerCase().includes(q)) ||
        (r.reviewerName && r.reviewerName.toLowerCase().includes(q)) ||
        (r.reviewerEmail && r.reviewerEmail.toLowerCase().includes(q)) ||
        (r.comment && r.comment.toLowerCase().includes(q))
      );
    });
  });

  // --- Coupons tab state ---
  coupons = signal<CouponResponse[]>([]);
  couponsLoading = signal(false);
  couponSaving = signal(false);
  isCouponModalOpen = signal(false);
  isEditingCoupon = signal(false);
  selectedCouponId = signal<number | null>(null);
  togglingCouponId = signal<number | null>(null);
  couponExpiryInput = '';
  couponForm: CouponRequest = {
    code: '',
    type: 'percent',
    value: 10,
    active: true,
    minOrderAmount: null,
    expiresAt: null
  };

  // Data
  products = signal<ProductResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  orders = signal<OrderResponse[]>([]);
  ordersLoading = signal(false);

  // Catalog-wide stock numbers from the server. The cards used to count only the products on
  // the CURRENT PAGE (10 at a time), so they were wrong for any catalog bigger than one page.
  stats = signal<ProductStats | null>(null);

  // --- Analytics tab ---
  analytics = signal<AnalyticsResponse | null>(null);
  analyticsLoading = signal(false);
  analyticsRangeDays = signal(30);

  maxDailyRevenue = computed(() => {
    const data = this.analytics();
    if (!data || data.revenueByDay.length === 0) return 0;
    return Math.max(...data.revenueByDay.map(p => p.revenue), 1);
  });

  // NEW: "Quick Insights" used to repeat the three cards above it. These are new facts.
  bestDay = computed(() => {
    const days = this.analytics()?.revenueByDay ?? [];
    if (days.length === 0) return null;
    const best = days.reduce((a, b) => (b.revenue > a.revenue ? b : a));
    return best.revenue > 0 ? best : null;
  });

  daysWithOrders = computed(
    () => (this.analytics()?.revenueByDay ?? []).filter(d => d.orderCount > 0).length
  );

  avgOrdersPerDay = computed(() => {
    const a = this.analytics();
    if (!a) return 0;
    return a.totalOrders / Math.max(1, this.analyticsRangeDays());
  });

  openAnalyticsTab(): void {
    this.activeTab.set('analytics');
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.analyticsLoading.set(true);
    this.analyticsService.getAnalytics(this.analyticsRangeDays()).subscribe({
      next: (res) => {
        this.analytics.set(res.data ?? null);
        this.analyticsLoading.set(false);
      },
      error: () => {
        this.analyticsLoading.set(false);
        this.toastService.error('Could not load analytics.');
      },
    });
  }

  setAnalyticsRange(days: number): void {
    this.analyticsRangeDays.set(days);
    this.loadAnalytics();
  }

  barHeightPercent(revenue: number): number {
    const max = this.maxDailyRevenue();
    if (max === 0) return 0;
    return Math.max(2, Math.round((revenue / max) * 100));
  }

  updatingOrderId = signal<number | null>(null);
  // NEW: the order whose refund request is running
  refundingOrderId = signal<number | null>(null);
  searchTerm = signal('');
  selectedStockFilter = signal<'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'>('ALL');

  // --- Quick Restock Modal State ---
  isRestockModalOpen = signal(false);
  restockingProduct = signal<ProductResponse | null>(null);
  restockQuantity = 10;
  variantRestocks: Record<number, number> = {};
  savingRestock = signal(false);

  page = signal(0);
  size = signal(10);
  totalPages = signal(0);
  totalElements = signal(0);

  // --- Orders tab: search + filters + paging (all in the browser) ---
  readonly ordersPageSize = 8;
  ordersCurrentPage = signal(1);

  orderSearch = signal('');
  orderStatusFilter = signal('ALL');
  orderPaymentFilter = signal('ALL');

  filteredOrders = computed(() => {
    const q = this.orderSearch().trim().toLowerCase().replace(/^#/, '');
    const status = this.orderStatusFilter();
    const pay = this.orderPaymentFilter();

    return this.orders().filter(o => {
      if (status !== 'ALL' && o.orderStatus !== status) return false;
      if (pay !== 'ALL' && o.paymentStatus !== pay) return false;
      if (!q) return true;
      return (
        String(o.orderId).includes(q) ||
        (o.customerName ?? '').toLowerCase().includes(q) ||
        (o.customerEmail ?? '').toLowerCase().includes(q) ||
        (o.phoneNumber ?? '').includes(q)
      );
    });
  });

  ordersTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredOrders().length / this.ordersPageSize))
  );

  // 1 2 3 … 16 instead of sixteen buttons in a row.
  ordersPageItems = computed<(number | '…')[]>(() => {
    const total = this.ordersTotalPages();
    const cur = this.ordersCurrentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);

    const wanted = new Set([1, total, cur - 1, cur, cur + 1]);
    const sorted = [...wanted].filter(p => p >= 1 && p <= total).sort((a, b) => a - b);
    const out: (number | '…')[] = [];
    sorted.forEach((p, i) => {
      if (i > 0 && p - sorted[i - 1] > 1) out.push('…');
      out.push(p);
    });
    return out;
  });

  pagedOrders = computed(() => {
    const start = (this.ordersCurrentPage() - 1) * this.ordersPageSize;
    return this.filteredOrders().slice(start, start + this.ordersPageSize);
  });

  readonly categoriesPageSize = 8;
  categoriesCurrentPage = signal(1);
  categoriesTotalPages = computed(() => Math.max(1, Math.ceil(this.categories().length / this.categoriesPageSize)));
  categoriesPageNumbers = computed(() => Array.from({ length: this.categoriesTotalPages() }, (_, i) => i + 1));
  pagedCategories = computed(() => {
    const start = (this.categoriesCurrentPage() - 1) * this.categoriesPageSize;
    return this.categories().slice(start, start + this.categoriesPageSize);
  });

  goToOrdersPage(p: number): void {
    if (p < 1 || p > this.ordersTotalPages()) return;
    this.ordersCurrentPage.set(p);
  }

  goToCategoriesPage(p: number): void {
    if (p < 1 || p > this.categoriesTotalPages()) return;
    this.categoriesCurrentPage.set(p);
  }

  onOrderSearch(value: string): void {
    this.orderSearch.set(value);
    this.ordersCurrentPage.set(1);
  }

  onOrderStatusFilter(value: string): void {
    this.orderStatusFilter.set(value);
    this.ordersCurrentPage.set(1);
  }

  onOrderPaymentFilter(value: string): void {
    this.orderPaymentFilter.set(value);
    this.ordersCurrentPage.set(1);
  }

  currentSort = 'price,asc';
  productForm: ProductRequest = this.initProductForm();
  categoryForm: CategoryRequest = { name: '', description: '' };
  editingCategoryId = signal<number | null>(null);

  // Search is debounced and out-of-order responses are ignored (see loadProducts).
  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private productRequestId = 0;

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loadProducts();
    this.loadStats();
    this.loadCategories();
    this.loadOrders();
    this.loadCoupons();
    this.loadReviews();
  }

  loadStats(): void {
    this.productService.getStats().subscribe({
      next: (res) => this.stats.set(res.data ?? null),
      error: () => this.stats.set(null),
    });
  }

  loadOrders(): void {
    this.ordersLoading.set(true);
    this.orderService.getAllOrders().subscribe({
      next: (res) => {
        const list = Array.isArray(res) ? res : (res?.data ?? []);
        this.orders.set(list);
        // A refresh can leave the current page past the end (e.g. after filtering).
        if (this.ordersCurrentPage() > this.ordersTotalPages()) {
          this.ordersCurrentPage.set(this.ordersTotalPages());
        }
        this.ordersLoading.set(false);
      },
      error: () => {
        this.ordersLoading.set(false);
        this.toastService.error('Could not load orders.');
      },
    });
  }

  // The statuses offered in an order's dropdown: its current one plus the legal next steps.
  statusChoices(order: OrderResponse): string[] {
    return [order.orderStatus, ...(NEXT_STATUSES[order.orderStatus] ?? [])];
  }

  // --- Plain-words labels for the orders table ---
  private isCod(o: OrderResponse): boolean {
    return (o.paymentMethod ?? '').toUpperCase() === 'COD';
  }

  methodLabel(o: OrderResponse): string {
    return this.isCod(o) ? 'Cash on delivery' : 'Card / online';
  }

  // COD orders sit at PENDING until the parcel is delivered. That is normal, so say so
  // instead of showing a bare "PENDING" that looks like a problem.
  paymentLabel(o: OrderResponse): string {
    switch (o.paymentStatus) {
      case 'PAID': return 'Paid';
      case 'REFUND_PENDING': return 'Refund pending';
      case 'REFUNDED': return 'Refunded';
      case 'PENDING': return this.isCod(o) ? 'Pay on delivery' : 'Awaiting payment';
      default: return o.paymentStatus;
    }
  }

  itemsSummary(o: OrderResponse): string {
    return (o.items ?? []).map(i => `${i.productName} × ${i.quantity}`).join(', ');
  }

  // NEW: sends the money for a cancelled, paid order back to the customer (full refund via Razorpay).
  async refundOrder(order: OrderResponse): Promise<void> {
    const ok = await this.confirmService.ask({
      title: `Refund order #${order.orderId}?`,
      message: `₹${order.totalAmount.toFixed(2)} will be sent back to the customer's original payment method ` +
        `through Razorpay, and the customer will be emailed. This can't be undone.`,
      confirmLabel: 'Refund',
      danger: true,
    });
    if (!ok) return;

    this.refundingOrderId.set(order.orderId);
    this.orderService.refundOrder(order.orderId).subscribe({
      next: (res) => {
        this.refundingOrderId.set(null);
        const updated: OrderResponse | undefined = res?.data;
        this.orders.update(list =>
          list.map(o => (o.orderId === order.orderId ? { ...o, ...(updated ?? { paymentStatus: 'REFUNDED' }) } : o))
        );
        this.toastService.success(`Order #${order.orderId} refunded.`);
      },
      error: (err) => {
        this.refundingOrderId.set(null);
        // The server explains why (e.g. "Razorpay could not process the refund right now: ...").
        this.toastService.error(err.error?.message || 'Could not refund this order.');
        this.loadOrders();
      },
    });
  }

  // Re-creates the table rows so a <select> the admin changed (and then declined to confirm)
  // snaps back to the real status without another trip to the server.
  private resetOrderRows(): void {
    this.orders.update(list => list.map(o => ({ ...o })));
  }

  // --- Dispatch Modal State ---
  isDispatchModalOpen = signal(false);
  dispatchOrderId = signal<number | null>(null);
  dispatchCarrier = 'BlueDart';
  dispatchTrackingNumber = '';

  openDispatchModal(orderId: number): void {
    this.dispatchOrderId.set(orderId);
    this.dispatchCarrier = 'BlueDart';
    this.dispatchTrackingNumber = '';
    this.isDispatchModalOpen.set(true);
  }

  closeDispatchModal(): void {
    this.isDispatchModalOpen.set(false);
    this.resetOrderRows();
  }

  confirmDispatch(): void {
    if (this.dispatchOrderId()) {
      const id = this.dispatchOrderId()!;
      this.isDispatchModalOpen.set(false);
      this.updateOrderStatus(id, 'SHIPPED', this.dispatchCarrier, this.dispatchTrackingNumber);
    }
  }

  // Called by the status dropdown. Risky changes ask first.
  async onOrderStatusChange(order: OrderResponse, status: string): Promise<void> {
    if (status === 'SHIPPED') {
      this.openDispatchModal(order.orderId);
      return;
    }

    if (status === 'CANCELLED') {
      const paidNote = order.paymentStatus === 'PAID'
        ? ' The customer has already paid, so the payment will need a refund.'
        : '';
      const ok = await this.confirmService.ask({
        title: 'Cancel this order?',
        message: `Order #${order.orderId} will be cancelled, its stock returned, and the customer emailed.${paidNote}`,
        confirmLabel: 'Cancel order',
        danger: true,
      });
      if (!ok) {
        this.resetOrderRows();
        return;
      }
    } else if (status === 'DELIVERED' && this.isCod(order) && order.paymentStatus !== 'PAID') {
      const ok = await this.confirmService.ask({
        title: 'Mark as delivered?',
        message: `This also records the cash on delivery payment for order #${order.orderId} as PAID.`,
        confirmLabel: 'Mark delivered',
      });
      if (!ok) {
        this.resetOrderRows();
        return;
      }
    }
    this.updateOrderStatus(order.orderId, status);
  }

  updateOrderStatus(orderId: number, status: string, carrier?: string, trackingNumber?: string): void {
    this.updatingOrderId.set(orderId);
    this.orderService.updateOrderStatus(orderId, status, carrier, trackingNumber).subscribe({
      next: (res) => {
        this.updatingOrderId.set(null);
        // Use the server's answer: delivering a COD order also flips its payment to PAID.
        const updated: OrderResponse | undefined = res?.data;
        this.orders.update(list =>
          list.map(o => (o.orderId === orderId ? { ...o, ...(updated ?? { orderStatus: status }) } : o))
        );
        this.toastService.success(`Order #${orderId} marked ${status}.`);
      },
      error: (err) => {
        this.updatingOrderId.set(null);
        // The server explains WHY (e.g. "This card order has not been paid yet"); show it.
        this.toastService.error(err.error?.message || 'Could not update order status.');
        // Put the dropdown back to the real status.
        this.loadOrders();
      },
    });
  }

  // --- Stock labels for the products table ---
  stockLabel(p: ProductResponse): string {
    if (p.stock === 0) return 'Out of stock';
    if (p.stock < 10) return `Low stock (${p.stock})`;
    return `In stock (${p.stock})`;
  }

  // What the customer pays for the base product (variant price overrides aside).
  finalPrice(p: ProductResponse): number {
    const d = p.discountPercentage ?? 0;
    if (d <= 0) return p.price;
    return Math.round((p.price - (p.price * d) / 100) * 100) / 100;
  }

  loadProducts(): void {
    const requestId = ++this.productRequestId;
    this.loading.set(true);
    this.productService
      .getFilteredProducts(
        this.searchTerm(),
        null,
        0,
        1_000_000,
        this.page(),
        this.size(),
        this.currentSort,
        this.selectedStockFilter()
      )
      .subscribe({
        next: (res) => {
          if (requestId !== this.productRequestId) return;   // a newer search already replaced this one
          const data = res.data as Page<ProductResponse> | undefined;
          this.products.set(data?.content || []);
          this.totalPages.set(data?.totalPages || 0);
          this.totalElements.set(data?.totalElements || 0);
          this.loading.set(false);
        },
        error: () => {
          if (requestId !== this.productRequestId) return;
          this.loading.set(false);
          this.toastService.error('Could not load products.');
        },
      });
  }

  setStockFilter(filter: 'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'): void {
    if (this.selectedStockFilter() === filter && filter !== 'ALL') {
      this.selectedStockFilter.set('ALL');
    } else {
      this.selectedStockFilter.set(filter);
    }
    this.page.set(0);
    this.loadProducts();
  }

  openRestockModal(p: ProductResponse): void {
    this.restockingProduct.set(p);
    this.restockQuantity = 10;
    this.variantRestocks = {};
    if (p.variants && p.variants.length > 0) {
      for (const v of p.variants) {
        this.variantRestocks[v.variantId] = 0;
      }
    }
    this.isRestockModalOpen.set(true);
  }

  closeRestockModal(): void {
    this.isRestockModalOpen.set(false);
    this.restockingProduct.set(null);
  }

  addRestockIncrement(amount: number): void {
    this.restockQuantity = Math.max(1, (this.restockQuantity || 0) + amount);
  }

  addVariantIncrement(variantId: number, amount: number): void {
    this.variantRestocks[variantId] = Math.max(0, (this.variantRestocks[variantId] || 0) + amount);
  }

  confirmRestock(): void {
    const p = this.restockingProduct();
    if (!p) return;

    const hasVariants = p.variants && p.variants.length > 0;
    let payload: RestockRequest;

    if (hasVariants) {
      const totalToAdd = Object.values(this.variantRestocks).reduce((a, b) => a + b, 0);
      if (totalToAdd <= 0) {
        this.toastService.error('Please specify units to add for at least one variant.');
        return;
      }
      payload = { variantRestocks: this.variantRestocks };
    } else {
      if (!this.restockQuantity || this.restockQuantity <= 0) {
        this.toastService.error('Restock quantity must be at least 1 unit.');
        return;
      }
      payload = { quantity: this.restockQuantity };
    }

    this.savingRestock.set(true);
    this.productService.restockProduct(p.productId, payload).subscribe({
      next: (res) => {
        this.savingRestock.set(false);
        this.isRestockModalOpen.set(false);
        const updated = res.data;
        if (updated) {
          this.toastService.success(`Restocked ${p.name}. Total stock: ${updated.stock} units.`);
        } else {
          this.toastService.success(`Restocked inventory for ${p.name}.`);
        }
        this.loadProducts();
        this.loadStats();
      },
      error: (err) => {
        this.savingRestock.set(false);
        this.toastService.error(err.error?.message || 'Could not restock product.');
      }
    });
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (res) => {
        this.categories.set(res.data || []);
        if (this.categoriesCurrentPage() > this.categoriesTotalPages()) {
          this.categoriesCurrentPage.set(this.categoriesTotalPages());
        }
      },
      error: () => this.toastService.error('Could not load categories.'),
    });
  }

  openAddProduct(): void {
    this.isEditMode.set(false);
    this.productForm = this.initProductForm();
    window.scrollTo({ top: 0, behavior: 'instant' });
    this.isModalOpen.set(true);
  }

  openEditProduct(p: ProductResponse): void {
    this.isEditMode.set(true);
    this.selectedId.set(p.productId);
    this.productForm = {
      name: p.name,
      description: p.description,
      price: p.price,
      stock: p.stock,
      imageUrl: p.imageUrl,
      imageUrls: p.imageUrls ? [...p.imageUrls] : (p.imageUrl ? [p.imageUrl] : []),
      status: p.status,
      // Ratings now come only from real customer reviews; the server ignores these two.
      rating: null,
      reviewCount: null,
      discountPercentage: p.discountPercentage ?? null,
      categoryId: p.categoryId,
      variants: (p.variants ?? []).map(v => ({
        variantId: v.variantId,
        size: v.size,
        color: v.color,
        stock: v.stock,
        priceOverride: v.priceOverride,
        sku: v.sku,
      })),
    };
    window.scrollTo({ top: 0, behavior: 'instant' });
    this.isModalOpen.set(true);
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    // Instant feedback; the server checks the real file contents again.
    if (file.size > 5 * 1024 * 1024) {
      this.toastService.error('Image is too large - max size is 5MB.');
      input.value = '';
      return;
    }

    this.uploadingImage.set(true);
    this.productService.uploadImage(file).subscribe({
      next: (res) => {
        this.uploadingImage.set(false);
        if (res.success && res.data?.imageUrl) {
          const img = res.data.imageUrl;
          if (!this.productForm.imageUrls) this.productForm.imageUrls = [];
          if (!this.productForm.imageUrls.includes(img)) {
            this.productForm.imageUrls.push(img);
          }
          if (!this.productForm.imageUrl) {
            this.productForm.imageUrl = img;
          }
          this.toastService.success('Image uploaded.');
        } else {
          this.toastService.error(res.message || 'Upload failed.');
        }
        input.value = '';
      },
      error: (err) => {
        this.uploadingImage.set(false);
        input.value = '';
        this.toastService.error(err.error?.message || 'Could not upload image.');
      },
    });
  }

  removeGalleryImage(index: number): void {
    if (!this.productForm.imageUrls) return;
    this.productForm.imageUrls.splice(index, 1);
    if (this.productForm.imageUrls.length > 0) {
      this.productForm.imageUrl = this.productForm.imageUrls[0];
    } else {
      this.productForm.imageUrl = '';
    }
  }

  setPrimaryImage(index: number): void {
    if (!this.productForm.imageUrls || !this.productForm.imageUrls[index]) return;
    this.productForm.imageUrl = this.productForm.imageUrls[index];
    this.toastService.info('Primary image set.');
  }

  addVariantRow(): void {
    this.productForm.variants.push({
      variantId: null,
      size: '',
      color: '',
      stock: 0,
      priceOverride: null,
      sku: '',
    });
  }

  removeVariantRow(index: number): void {
    this.productForm.variants.splice(index, 1);
  }

  // Same rules the server enforces, so mistakes are caught before the confirm dialog.
  private validateProductForm(): string | null {
    const f = this.productForm;
    const name = (f.name ?? '').trim();
    if (name.length < 3 || name.length > 100) return 'Product name must be 3 to 100 characters.';
    if (!(f.description ?? '').trim()) return 'Please add a description.';
    if (f.description.length > 500) return 'Description must be 500 characters or fewer.';
    if (f.price == null || f.price < 0.01) return 'Price must be at least 0.01.';
    if (!f.categoryId || f.categoryId <= 0) return 'Please choose a category.';
    if (!f.imageUrl) return 'Please upload a product image.';
    if (f.discountPercentage != null && (f.discountPercentage < 0 || f.discountPercentage > 90)) {
      return 'Discount must be between 0 and 90%.';
    }
    if (f.variants.length === 0 && (f.stock == null || f.stock < 0)) return 'Stock cannot be negative.';
    for (const v of f.variants) {
      if (!(v.size ?? '').trim() && !(v.color ?? '').trim()) return 'Each variant needs a size or a color.';
      if (v.stock == null || v.stock < 0) return 'Variant stock cannot be negative.';
      if (v.priceOverride != null && v.priceOverride < 0.01) return 'A variant price override must be at least 0.01 (or leave it empty).';
    }
    return null;
  }

  async saveProduct(): Promise<void> {
    const problem = this.validateProductForm();
    if (problem) {
      this.toastService.error(problem);
      return;
    }

    const confirmed = await this.confirmService.ask({
      title: this.isEditMode() ? 'Save changes?' : 'Create product?',
      message: this.isEditMode()
        ? 'This will update the product for all customers immediately.'
        : 'This will add a new product to your store.',
      confirmLabel: this.isEditMode() ? 'Save changes' : 'Create product',
    });
    if (!confirmed) return;

    const request = this.isEditMode()
      ? this.productService.updateProduct(this.selectedId()!, this.productForm)
      : this.productService.createProduct(this.productForm);

    this.loading.set(true);
    request.subscribe({
      next: (res) => {
        this.loading.set(false);
        // A duplicate product name comes back as HTTP 200 with success:false. This used to be
        // reported as "Product created" and the form closed, although nothing was saved.
        if (!res.success) {
          this.toastService.error(res.message || 'Could not save the product.');
          return;
        }
        this.isModalOpen.set(false);
        this.toastService.success(this.isEditMode() ? 'Product updated.' : 'Product created.');
        this.loadProducts();
        this.loadStats();
      },
      error: (err) => {
        this.loading.set(false);
        this.toastService.error(err.error?.message || 'Failed to save product.');
      },
    });
  }

  saveCategory(): void {
    if (!this.categoryForm.name.trim()) return;
    const editingId = this.editingCategoryId();

    const request = editingId
      ? this.categoryService.updateCategory(editingId, this.categoryForm)
      : this.categoryService.createCategory(this.categoryForm);

    request.subscribe({
      next: () => {
        this.categoryForm = { name: '', description: '' };
        this.editingCategoryId.set(null);
        this.toastService.success(editingId ? 'Category updated.' : 'Category created.');
        this.loadCategories();
      },
      error: (err) => {
        this.toastService.error(
          err.error?.message || (editingId ? 'Failed to update category.' : 'Failed to create category.')
        );
      },
    });
  }

  editCategory(category: CategoryResponse): void {
    this.editingCategoryId.set(category.categoryId);
    this.categoryForm = { name: category.name, description: category.description };
  }

  cancelEditCategory(): void {
    this.editingCategoryId.set(null);
    this.categoryForm = { name: '', description: '' };
  }

  async deleteCategory(category: CategoryResponse): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: 'Delete this category?',
      message: `This will permanently delete "${category.name}". Categories that still have products in them can't be deleted.`,
      confirmLabel: 'Delete',
      danger: true,
    });
    if (!confirmed) return;

    this.categoryService.deleteCategory(category.categoryId).subscribe({
      next: () => {
        this.toastService.success('Category deleted.');
        if (this.editingCategoryId() === category.categoryId) {
          this.cancelEditCategory();
        }
        this.loadCategories();
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Could not delete category.');
      },
    });
  }

  async deleteProduct(id: number): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: 'Delete this product?',
      message: 'This will permanently remove the product from your store. This can\'t be undone.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (!confirmed) return;

    this.loading.set(true);
    this.productService.deleteProduct(id).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (!res.success) {
          this.toastService.error(res.message || 'Could not delete product.');
          return;
        }
        this.toastService.success('Product deleted.');
        this.loadProducts();
        this.loadStats();
      },
      error: (err) => {
        this.loading.set(false);
        this.toastService.error(err.error?.message || 'Could not delete product.');
      },
    });
  }

  // Waits 300 ms after the last keystroke instead of sending a request for every letter.
  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.page.set(0);
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.loadProducts(), 300);
  }

  goPrevPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.loadProducts();
    }
  }

  goNextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.set(this.page() + 1);
      this.loadProducts();
    }
  }

  changeSort(sort: string): void {
    this.currentSort = sort;
    this.page.set(0);
    this.loadProducts();
  }

  openCouponsTab(): void {
    this.activeTab.set('coupons');
    this.loadCoupons();
  }

  openReviewsTab(): void {
    this.activeTab.set('reviews');
    this.loadReviews();
  }

  loadReviews(): void {
    this.reviewsLoading.set(true);
    this.reviewService.getAllReviewsForAdmin().subscribe({
      next: (res) => {
        this.adminReviews.set(res.data ?? []);
        this.reviewsLoading.set(false);
      },
      error: () => {
        this.reviewsLoading.set(false);
        this.toastService.error('Could not load reviews.');
      }
    });
  }

  onReviewSearchChange(value: string): void {
    this.reviewSearch.set(value);
  }

  onReviewRatingChange(value: string | number): void {
    this.reviewRatingFilter.set(value === 'ALL' ? 'ALL' : Number(value));
  }

  async deleteReviewAdmin(review: AdminReviewResponse): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: `Delete review by ${review.reviewerName || 'Customer'}?`,
      message: `Are you sure you want to delete this ${review.rating}-star review for "${review.productName}"? The product's rating score will be recalculated immediately.`,
      confirmLabel: 'Delete review',
      danger: true
    });
    if (!confirmed) return;

    this.deletingReviewId.set(review.reviewId);
    this.reviewService.deleteReviewById(review.reviewId).subscribe({
      next: () => {
        this.deletingReviewId.set(null);
        this.toastService.success('Review deleted and product rating updated.');
        this.loadReviews();
        this.loadProducts();
      },
      error: (err) => {
        this.deletingReviewId.set(null);
        this.toastService.error(err.error?.message || 'Failed to delete review.');
      }
    });
  }

  loadCoupons(): void {
    this.couponsLoading.set(true);
    this.couponService.getAllCoupons().subscribe({
      next: (res) => {
        this.coupons.set(res.data ?? []);
        this.couponsLoading.set(false);
      },
      error: () => {
        this.couponsLoading.set(false);
        this.toastService.error('Could not load coupons.');
      }
    });
  }

  openCreateCouponModal(): void {
    this.isEditingCoupon.set(false);
    this.selectedCouponId.set(null);
    this.couponExpiryInput = '';
    this.couponForm = {
      code: '',
      type: 'percent',
      value: 10,
      active: true,
      minOrderAmount: null,
      expiresAt: null
    };
    this.isCouponModalOpen.set(true);
  }

  openEditCouponModal(coupon: CouponResponse): void {
    this.isEditingCoupon.set(true);
    this.selectedCouponId.set(coupon.id);
    this.couponExpiryInput = coupon.expiresAt ? coupon.expiresAt.substring(0, 16) : '';
    this.couponForm = {
      code: coupon.code,
      type: coupon.type,
      value: coupon.displayValue,
      active: coupon.active,
      minOrderAmount: coupon.minOrderAmount,
      expiresAt: coupon.expiresAt
    };
    this.isCouponModalOpen.set(true);
  }

  closeCouponModal(): void {
    this.isCouponModalOpen.set(false);
  }

  async saveCoupon(): Promise<void> {
    const f = this.couponForm;
    const code = (f.code ?? '').trim().toUpperCase();
    if (code.length < 3 || code.length > 20) {
      this.toastService.error('Coupon code must be between 3 and 20 characters.');
      return;
    }
    if (!/^[A-Z0-9_-]+$/.test(code)) {
      this.toastService.error('Coupon code may only contain letters, numbers, hyphens, and underscores.');
      return;
    }
    if (f.value == null || f.value <= 0) {
      this.toastService.error('Discount value must be greater than 0.');
      return;
    }
    if (f.type === 'percent' && f.value > 100) {
      this.toastService.error('Percentage discount cannot exceed 100%.');
      return;
    }

    const payload: CouponRequest = {
      code: code,
      type: f.type,
      value: f.value,
      active: f.active,
      minOrderAmount: f.minOrderAmount && f.minOrderAmount > 0 ? f.minOrderAmount : null,
      expiresAt: this.couponExpiryInput ? this.couponExpiryInput : null
    };

    this.couponSaving.set(true);
    const request = this.isEditingCoupon()
      ? this.couponService.updateCoupon(this.selectedCouponId()!, payload)
      : this.couponService.createCoupon(payload);

    request.subscribe({
      next: () => {
        this.couponSaving.set(false);
        this.isCouponModalOpen.set(false);
        this.toastService.success(this.isEditingCoupon() ? 'Coupon updated.' : 'Coupon created.');
        this.loadCoupons();
      },
      error: (err) => {
        this.couponSaving.set(false);
        this.toastService.error(err.error?.message || 'Failed to save coupon.');
      }
    });
  }

  toggleCouponActive(coupon: CouponResponse): void {
    this.togglingCouponId.set(coupon.id);
    this.couponService.toggleActive(coupon.id).subscribe({
      next: (res) => {
        this.togglingCouponId.set(null);
        if (res.data) {
          const updated = res.data;
          this.coupons.update(list => list.map(c => c.id === coupon.id ? updated : c));
          this.toastService.success(`Coupon ${coupon.code} marked ${updated.active ? 'Active' : 'Inactive'}.`);
        } else {
          this.loadCoupons();
        }
      },
      error: (err) => {
        this.togglingCouponId.set(null);
        this.toastService.error(err.error?.message || 'Could not toggle coupon status.');
      }
    });
  }

  async deleteCoupon(coupon: CouponResponse): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: `Delete coupon "${coupon.code}"?`,
      message: 'This promotional code will be permanently removed. Customers will no longer be able to apply it.',
      confirmLabel: 'Delete coupon',
      danger: true
    });
    if (!confirmed) return;

    this.couponService.deleteCoupon(coupon.id).subscribe({
      next: () => {
        this.toastService.success(`Coupon "${coupon.code}" deleted.`);
        this.loadCoupons();
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Could not delete coupon.');
      }
    });
  }

  private initProductForm(): ProductRequest {
    return {
      name: '',
      description: '',
      price: 0,
      stock: 0,
      imageUrl: '',
      imageUrls: [],
      status: ProductStatus.IN_STOCK,
      rating: null,
      reviewCount: null,
      discountPercentage: null,
      categoryId: 0,
      variants: [],
    };
  }
}