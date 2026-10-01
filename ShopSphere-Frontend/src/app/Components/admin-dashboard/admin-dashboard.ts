// 
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Page } from '../../Services/product-service';
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
  protected readonly orderStatusOptions = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

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
  inStockCount = computed(() =>
    this.products().filter(p => p.stock > 0).length
  );

  outOfStockCount = computed(() =>
    this.products().filter(p => p.stock === 0).length
  );

  lowStockCount = computed(() =>
    this.products().filter(
      p => p.stock > 0 && p.stock <= 10
    ).length
  );

  loadAnalytics(): void {
    this.analyticsLoading.set(true);
    this.analyticsService.getAnalytics(this.analyticsRangeDays()).subscribe({
      next: (res) => {
        this.analytics.set(res.data ?? null);
        this.analyticsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load analytics', err);
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

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loadProducts();
    this.loadCategories();
    this.loadOrders();
  }

  loadOrders(): void {
    this.ordersLoading.set(true);
    this.orderService.getAllOrders().subscribe({
      next: (res) => {
        const list = Array.isArray(res) ? res : (res?.data ?? []);
        this.orders.set(list);
        this.ordersLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load orders', err);
        this.ordersLoading.set(false);
        this.toastService.error('Could not load orders.');
      },
    });
  }

  updateOrderStatus(orderId: number, status: string): void {
    this.updatingOrderId.set(orderId);
    this.orderService.updateOrderStatus(orderId, status).subscribe({
      next: () => {
        this.updatingOrderId.set(null);
        this.orders.update(list =>
          list.map(o => (o.orderId === orderId ? { ...o, orderStatus: status } : o))
        );
        this.toastService.success(`Order #${orderId} marked ${status}.`);
      },
      error: (err) => {
        this.updatingOrderId.set(null);
        console.error('Failed to update order status', err);
        this.toastService.error('Could not update order status.');
      },
    });
  }

  loadProducts(): void {
    this.loading.set(true);

    this.productService
      .getFilteredProducts(
        this.searchTerm(),
        null,
        0,
        1_000_000,
        this.page(),
        this.size(),
        this.currentSort
      )
      .subscribe({
        next: (res) => {
          const data = res.data as Page<ProductResponse> | undefined;
          this.products.set(data?.content || []);
          this.totalPages.set(data?.totalPages || 0);
          this.totalElements.set(data?.totalElements || 0);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
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
    });
  }

  openAddProduct(): void {
    this.isEditMode.set(false);
    this.productForm = this.initProductForm();
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
      rating: p.rating ?? null,
      reviewCount: p.reviewCount ?? null,
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
    this.isModalOpen.set(true);
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

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

  async saveProduct(): Promise<void> {
    console.log('SAVE BUTTON CLICKED');
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
      next: () => {
        this.isModalOpen.set(false);
        this.loading.set(false);
        this.toastService.success(this.isEditMode() ? 'Product updated.' : 'Product created.');
        this.loadProducts();
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
      next: () => {
        this.loading.set(false);
        this.toastService.success('Product deleted.');
        this.loadProducts();
      },
      error: (err) => {
        this.loading.set(false);
        this.toastService.error(err.error?.message || 'Could not delete product.');
      },
    });
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.page.set(0);
    this.loadProducts();
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
  // Example inside your AdminDashboard component:
  openAddProductModal() {
    window.scrollTo({ top: 0, behavior: 'instant' }); // Resets page scroll so modal is immediately visible
    this.isModalOpen.set(true);
  }

  openEditProductModal(product: any) {
    window.scrollTo({ top: 0, behavior: 'instant' }); // Resets page scroll so modal is immediately visible
    this.isModalOpen.set(true);
  }

}