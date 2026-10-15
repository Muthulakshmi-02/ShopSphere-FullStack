import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Page, ProductStats } from '../../Services/product-service';
import { CategoryService } from '../../Services/category-service';
import {
  ProductResponse,
  ProductRequest,
  ProductStatus,
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
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmService);

  protected readonly productImageUrl = productImageUrl;

  // View state
  activeTab = signal<'products' | 'categories' | 'orders' | 'analytics' | ''>('products');
  isModalOpen = signal(false);
  isEditMode = signal(false);
  selectedId = signal<number | null>(null);
  loading = signal(false);
  uploadingImage = signal(false);

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
  searchTerm = signal('');
  page = signal(0);
  size = signal(10);
  totalPages = signal(0);
  totalElements = signal(0);

  readonly ordersPageSize = 8;
  ordersCurrentPage = signal(1);
  ordersTotalPages = computed(() => Math.max(1, Math.ceil(this.orders().length / this.ordersPageSize)));
  ordersPageNumbers = computed(() => Array.from({ length: this.ordersTotalPages() }, (_, i) => i + 1));
  pagedOrders = computed(() => {
    const start = (this.ordersCurrentPage() - 1) * this.ordersPageSize;
    return this.orders().slice(start, start + this.ordersPageSize);
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

  updateOrderStatus(orderId: number, status: string): void {
    this.updatingOrderId.set(orderId);
    this.orderService.updateOrderStatus(orderId, status).subscribe({
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

  loadProducts(): void {
    const requestId = ++this.productRequestId;
    this.loading.set(true);
    this.productService
      .getFilteredProducts(this.searchTerm(), null, 0, 1_000_000, this.page(), this.size(), this.currentSort)
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
          this.productForm.imageUrl = res.data.imageUrl;
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

  private initProductForm(): ProductRequest {
    return {
      name: '',
      description: '',
      price: 0,
      stock: 0,
      imageUrl: '',
      status: ProductStatus.IN_STOCK,
      rating: null,
      reviewCount: null,
      discountPercentage: null,
      categoryId: 0,
      variants: [],
    };
  }
  // The leftover openAddProductModal() / openEditProductModal(product: any) at the bottom were
  // removed: they only opened the modal WITHOUT resetting or filling the form, and nothing used them.
}