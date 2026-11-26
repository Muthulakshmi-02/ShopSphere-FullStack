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
  currentPage = signal<number>(0);
  pageSize = signal<number>(12);

  // Stock status filter (ALL, IN, LOW, OUT)
  stockFilter = signal<string>('ALL');

  // Helper to set stock filter and reset pagination
  setStockFilter(filter: string): void {
    this.stockFilter.set(filter);
    this.currentPage.set(0);
    this.loadProducts();
  }

  // Derived list after applying rating filter
  filteredProducts = computed<ProductResponse[]>(() => {
    const list = this.products();
    const rating = this.ratingFilter();
    if (rating === null) return list;
    return list.filter(p => {
      const r = Math.round(p.rating?.rate ?? 0);
      return r === rating;
    });

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
  });


  // CHANGED: "1 2 3 … 12" instead of one button per page.
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

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
    return out;
  });

  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  // Slower, older responses must not overwrite a newer one.
  private requestId = 0;

  sortOptions: SortOption[] = [
    { value: 'price,asc', label: 'Price: Low to High' },
    { value: 'price,desc', label: 'Price: High to Low' },
    { value: 'name,asc', label: 'Name: A to Z' },
    { value: 'name,desc', label: 'Name: Z to A' },
  ];

  constructor() {
    effect(
      () => {
        this.searchKeyword();
        this.selectedCategory();
        this.selectedSort();
        this.currentPage();
        this.pageSize();
        this.loadProducts();
        this.syncFiltersToUrl();
      },
      { allowSignalWrites: true },
    );
  }

  ngOnInit() {
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

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }

    // Wishlist is a per-customer feature, so only fetch it for a logged-in customer.
    if (this.authService.isLoggedIn() && !this.authService.isAdmin()) {
      this.wishlistService.loadWishlistIds().subscribe({
        error: (err) => console.error('Failed to load wishlist', err),
      });

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
    }
  }

  loadCategories() {
    this.categoryService.getAllCategories().subscribe({
      next: (res) => this.categories.set(res.data || []),
      error: (err) => console.error('Failed to load categories', err),
    });

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
  }

  loadProducts() {
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
        next: (res) => {
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
        error: (err) => {
          if (requestId !== this.requestId) return;
          console.error('Failed to load products', err);
          this.loading.set(false);
          this.loadError.set(true);
        },
      });

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
  }

  // ---------- Event handlers ----------
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

  // NEW: the hero's "Shop Collections" link used href="#product-grid". In an Angular app that
  // resolves against the base URL (it goes to "/#product-grid"), so it could leave this page
  // instead of scrolling. This scrolls smoothly and stays put.
  onPriceRangeChange(): void {
    // Sync the combined range signal with the individual min/max signals
    const [min, max] = this.priceRange();
    this.priceMin.set(min);
    this.priceMax.set(max);
    // Reset to first page when price filter changes
    this.currentPage.set(0);
    this.loadProducts();
  }


    event?.preventDefault();
    if (typeof document === 'undefined') return;
    document.getElementById('product-grid')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  // NEW: the three "Trending Now" items in the hero looked clickable (they even slide on hover) but
  // did nothing. They now open that category.
  selectCategoryByName(name: string): void {
    const wanted = name.trim().toLowerCase();
    const category = this.categories().find(c => c.name.trim().toLowerCase() === wanted);
    if (category) this.selectCategory(category.categoryId);
    this.scrollToGrid();
  }

  // ---------- Who may shop ----------
  // The server only lets customers buy (cart, checkout, wishlist). An admin who clicked these used to
  // get a confusing "not authorized" message; now they are told why.
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

  // ---------- Wishlist ----------
  isInWishlist(productId: number): boolean {
    return this.wishlistService.isWishlisted(productId);
  }

  toggleWishlist(productId: number): void {
    // CHANGED: a guest is now sent to sign in (and brought back), like Add to Cart does.
    if (!this.authService.isLoggedIn()) {
      this.sendToLogin('Please sign in to use your wishlist.');
      return;
    }
    if (this.blockedForAdmin()) return;

    this.wishlistService.toggle(productId).subscribe({
      next: (res) => {
        this.toastService.success(res.data ? 'Added to wishlist.' : 'Removed from wishlist.');
      },
      error: (err) => this.toastService.error(err.error?.message || 'Could not update wishlist.'),
    });

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
  }

  // ---------- Pricing ----------
  // CHANGED: rounded to paise (half up), like the server, so the card never shows a different
  // amount than the one that is charged.
  discountedPrice(product: ProductResponse): number {
    if (!product.discountPercentage) return product.price;
    const value = product.price - (product.price * product.discountPercentage) / 100;
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }

  // ---------- Cart Logic ----------
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
      error: (err) => {
        this.toastService.error(err.error?.message || 'Could not add item to cart.');
      }
    });

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
  }

  buyNow(productId: number): void {
    if (this.blockedForAdmin()) return;

    const product = this.products().find(p => p.productId === productId);
    if (product?.variants && product.variants.length > 0) {
      this.toastService.info('This product has options - pick one on the product page.');
      this.router.navigate(['/products', productId]);
      return;
    }

    // Guests are sent to login by the route guard and come back to checkout afterwards.
    this.router.navigate(['/checkout'], {
      queryParams: { buyNow: productId, quantity: 1 }
    });

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
  }

  // ---------- Paging & Helpers ----------
  clearFilters(): void {
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchInput.set('');
    this.searchKeyword.set('');
    this.selectedCategory.set(null);
    this.selectedSort.set('price,asc');
    this.currentPage.set(0);
  }

  onSortChange(sortValue: string): void {
    this.selectedSort.set(sortValue);
    this.currentPage.set(0);
  }

  selectCategory(categoryId: number | null): void {
    this.selectedCategory.set(categoryId);
    this.currentPage.set(0);
  }

  trackByProductId(index: number, product: ProductResponse): number {
    return product.productId;
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

  // Selected products for comparison (max 4)
  selectedForCompare = signal<number[]>([]);

  // Toggle a product's selection for comparison
  toggleComparison(productId: number): void {
    const current = this.selectedForCompare();
    if (current.includes(productId)) {
      this.selectedForCompare.set(current.filter(id => id !== productId));
    } else if (current.length < 4) {
      this.selectedForCompare.set([...current, productId]);
    } else {
      this.toastService.warn('You can compare up to 4 products');
    }
  }

  // Navigate to comparison view
  goToCompare(): void {
    const ids = this.selectedForCompare().join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }


  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(rating);
    this.currentPage.set(0);
    this.loadProducts();
  }
  }
}
