// Angular component for the product dashboard with filters, wishlist, and comparison feature
import { Component, OnInit, signal, computed, inject, effect } from '@angular/core';
import { ProductService, Page } from '../../Services/product-service';
import { CategoryService } from '../../Services/category-service';
import { CartService } from '../../Services/cart-service';
import { ProductResponse } from '../../Models/Product.model';
import { CategoryResponse } from '../../Models/Category.model';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { CommonModule, CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../shared/toast/toast-service';
import { WishlistService } from '../../Services/wishlist-service';
import { AuthService } from '../../Services/auth-service';
import { productImageUrl } from '../../shared/api.config';

export interface SortOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-product-dashboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, FormsModule, NgFor, NgIf, RouterLink],
  templateUrl: './product-dashboard.html',
  styleUrls: ['./product-dashboard.css'],
})
export class ProductDashboard implements OnInit {
  // Services
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private cartService = inject(CartService);
  private toastService = inject(ToastService);
  private wishlistService = inject(WishlistService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  protected readonly productImageUrl = productImageUrl;

  // ---------- State ----------
  products = signal<ProductResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  totalProducts = signal<number>(0);
  totalPages = signal<number>(0);
  loading = signal<boolean>(false);
  loadError = signal<boolean>(false);

  // ---------- Filters & pagination ----------
  searchInput = signal<string>('');
  searchKeyword = signal<string>('');
  selectedCategory = signal<number | null>(null);
  selectedSort = signal<string>('price,asc');
  // Options for sorting the product list (used in template)
  sortOptions: SortOption[] = [
    { value: 'price,asc', label: 'Price: Low to High' },
    { value: 'price,desc', label: 'Price: High to Low' },
    { value: 'name,asc', label: 'Name: A to Z' },
    { value: 'name,desc', label: 'Name: Z to A' }
  ];
  currentPage = signal<number>(0);
  pageSize = signal<number>(12);

  // Stock status filter (ALL, IN, LOW, OUT)
  stockFilter = signal<string>('ALL');

  // Rating filter (null = no filter)
  ratingFilter = signal<number | null>(null);

  // Price range signals – assume they exist elsewhere in the component
  priceRange = signal<[number, number]>([0, 1000]);
  priceMin = signal<number>(0);
  priceMax = signal<number>(1000);

  // Comparison feature – selected product IDs (max 4)
  selectedForCompare = signal<number[]>([]);

  // ---------- Computed values ----------
  filteredProducts = computed<ProductResponse[]>(() => {
    const list = this.products();
    const rating = this.ratingFilter();
    if (rating === null) return list;
    return list.filter(p => Math.round(p.rating ?? 0) === rating);
  });

  pageItems = computed<(number | '…')[]>(() => {
    const total = this.totalPages();
    const cur = this.currentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const wanted = new Set([0, total - 1, cur - 1, cur, cur + 1]);
    const sorted = [...wanted].filter(p => p >= 0 && p < total).sort((a, b) => a - b);
    const out: (number | '…')[] = [];
    sorted.forEach((p, i) => {
      if (i > 0 && p - sorted[i - 1] > 1) out.push('…');
      out.push(p);
    });
    return out;
  });

  // ---------- Lifecycle ----------
  constructor() {
    effect(() => {
      this.searchKeyword();
      this.selectedCategory();
      this.selectedSort();
      this.currentPage();
      this.pageSize();
      this.loadProducts();
      this.syncFiltersToUrl();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadCategories();
    this.route.queryParams.subscribe(params => {
      const search = params['search'] || '';
      if (search !== this.searchKeyword()) {
        this.searchKeyword.set(search);
        this.searchInput.set(search);
      }
      this.selectedCategory.set(params['category'] ? Number(params['category']) : null);
      this.selectedSort.set(params['sort'] || 'price,asc');
    });
  }

  // ---------- Data loading ----------
  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: res => this.categories.set(res.data || []),
      error: err => console.error('Failed to load categories', err),
    });
  }

  loadProducts(): void {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.loadError.set(false);
    this.productService
      .getFilteredProducts(
        this.searchKeyword() || '',
        this.selectedCategory(),
        this.priceRange()[0],
        this.priceRange()[1],
        this.currentPage(),
        this.pageSize(),
        this.selectedSort() || 'price,asc',
        this.stockFilter(),
      )
      .subscribe({
        next: res => {
          if (requestId !== this.requestId) return;
          const page = (res.data as Page<ProductResponse>) || { content: [], totalElements: 0, totalPages: 1 };
          const content = page.content || [];
          const totalElements = page.totalElements ?? content.length;
          const totalPages = page.totalPages ?? 1;
          this.products.set(content);
          this.totalProducts.set(totalElements);
          this.totalPages.set(totalPages);
          this.loading.set(false);
        },
        error: err => {
          if (requestId !== this.requestId) return;
          console.error('Failed to load products', err);
          this.loading.set(false);
          this.loadError.set(true);
        },
      });
  }

  // ---------- Event handlers ----------
  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private requestId = 0;

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value || '';
    this.searchInput.set(value);
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.searchKeyword.set(value.trim());
      this.currentPage.set(0);
    }, 300);
  }

  onSortChangeSafe(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.onSortChange(target.value);
  }

  onSortChange(sortValue: string): void {
    this.selectedSort.set(sortValue);
    this.currentPage.set(0);
  }

  onPriceRangeChange(): void {
    const [min, max] = this.priceRange();
    this.priceMin.set(min);
    this.priceMax.set(max);
    this.currentPage.set(0);
    this.loadProducts();
  }

  // ---------- Filter helpers ----------
  setStockFilter(filter: string): void {
    this.stockFilter.set(filter);
    this.currentPage.set(0);
    this.loadProducts();
  }

  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }

  // ---------- Comparison feature ----------
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.info('You can compare up to 4 products');
    }
  }

  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }

  // ---------- Wishlist ----------
  isInWishlist(productId: number): boolean {
    return this.wishlistService.isWishlisted(productId);
  }

  toggleWishlist(productId: number): void {
    if (!this.authService.isLoggedIn()) {
      this.sendToLogin('Please sign in to use your wishlist.');
      return;
    }
    if (this.blockedForAdmin()) return;
    this.wishlistService.toggle(productId).subscribe({
      next: res => this.toastService.success(res.data ? 'Added to wishlist.' : 'Removed from wishlist.'),
      error: err => this.toastService.error(err.error?.message || 'Could not update wishlist.'),
    });
  }

  // ---------- Cart ----------
  addToCart(productId: number): void {
    if (!this.authService.isLoggedIn()) {
      this.sendToLogin('Please sign in to add items to your cart.');
      return;
    }
    if (this.blockedForAdmin()) return;
    const product = this.products().find(p => p.productId === productId);
    if (product?.variants && product.variants.length > 0) {
      this.toastService.info('This product has options - pick one on the product page.');
      this.router.navigate(['/products', productId]);
      return;
    }
    this.cartService.addItemToCart(productId, 1).subscribe({
      next: () => this.toastService.success('Item added to your cart!'),
      error: err => this.toastService.error(err.error?.message || 'Could not add item to cart.'),
    });
  }

  buyNow(productId: number): void {
    if (this.blockedForAdmin()) return;
    const product = this.products().find(p => p.productId === productId);
    if (product?.variants && product.variants.length > 0) {
      this.toastService.info('This product has options - pick one on the product page.');
      this.router.navigate(['/products', productId]);
      return;
    }
    this.router.navigate(['/checkout'], { queryParams: { buyNow: productId, quantity: 1 } });
  }

  // ---------- Admin guard ----------
  private blockedForAdmin(): boolean {
    if (this.authService.isLoggedIn() && this.authService.isAdmin()) {
      this.toastService.info('You are signed in as an administrator. Sign in with a customer account to shop.');
      return true;
    }
    return false;
  }

  private sendToLogin(message: string): void {
    this.toastService.info(message);
    this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
  }

  // ---------- Pagination ----------
  clearFilters(): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchInput.set('');
    this.searchKeyword.set('');
    this.selectedCategory.set(null);
    this.selectedSort.set('price,asc');
    this.currentPage.set(0);
  }

  goPrevPage(): void {
    if (this.currentPage() > 0) this.currentPage.set(this.currentPage() - 1);
  }

  goNextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) this.currentPage.set(this.currentPage() + 1);
  }

  goToPage(pageIndex: number): void {
    this.currentPage.set(pageIndex);
  }

  // ---------- Helpers ----------
  selectCategoryByName(name: string): void {
    const wanted = name.trim().toLowerCase();
    const category = this.categories().find(c => c.name.trim().toLowerCase() === wanted);
    if (category) this.selectCategory(category.categoryId);
    this.scrollToGrid();
  }

  selectCategory(categoryId: number | null): void {
    this.selectedCategory.set(categoryId);
    this.currentPage.set(0);
  }

  scrollToGrid(): void {
    const el = document.getElementById('product-grid');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  trackByProductId(index: number, product: ProductResponse): number {
    return product.productId;
  }

  // ---------- URL sync ----------
  private syncFiltersToUrl(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        search: this.searchKeyword() || null,
        category: this.selectedCategory(),
        sort: this.selectedSort(),
      },
      replaceUrl: true,
    });
  }

  // ---------- Pricing ----------
  discountedPrice(product: ProductResponse): number {
    if (!product.discountPercentage) return product.price;
    const value = product.price - (product.price * product.discountPercentage) / 100;
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }
}
