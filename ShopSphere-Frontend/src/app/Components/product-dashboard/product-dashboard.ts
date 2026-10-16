import { Component, OnInit, signal, inject, effect } from '@angular/core';
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

  // Same image helper the cart and detail pages use (was: a hard-coded http://localhost:8080 here).
  protected readonly productImageUrl = productImageUrl;

  // ---------- State ----------
  products = signal<ProductResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  totalProducts = signal<number>(0);
  totalPages = signal<number>(0);
  loading = signal<boolean>(false);
  loadError = signal<boolean>(false);
  pageNumbers = signal<number[]>([]);

  // ---------- Filters & pagination ----------
  // searchInput = what is in the box right now; searchKeyword = what the list is filtered by
  // (it follows searchInput 300 ms after the last keystroke).
  searchInput = signal<string>('');
  searchKeyword = signal<string>('');
  selectedCategory = signal<number | null>(null);
  selectedSort = signal<string>('price,asc');
  currentPage = signal<number>(0);
  pageSize = signal<number>(12);

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
      // Only when the URL differs from what is applied (back button, shared link). Without this
      // check, the URL update that follows each search would reset the text box while typing.
      if (search !== this.searchKeyword()) {
        this.searchKeyword.set(search);
        this.searchInput.set(search);
      }

      this.selectedCategory.set(params['category'] ? Number(params['category']) : null);
      this.selectedSort.set(params['sort'] || 'price,asc');
    });

    // Restored: the rewritten ngOnInit had dropped this, so hearts were never filled after a
    // refresh. Wishlist is a per-customer feature, so only fetch it for a logged-in customer.
    if (this.authService.isLoggedIn() && !this.authService.isAdmin()) {
      this.wishlistService.loadWishlistIds().subscribe({
        error: (err) => console.error('Failed to load wishlist', err),
      });
    }
  }

  loadCategories() {
    this.categoryService.getAllCategories().subscribe({
      next: (res) => this.categories.set(res.data || []),
      error: (err) => console.error('Failed to load categories', err),
    });
  }

  loadProducts() {
    const requestId = ++this.requestId;
    this.loading.set(true);
    this.loadError.set(false);

    this.productService
      .getFilteredProducts(
        this.searchKeyword() || '',
        this.selectedCategory(),
        0,
        1_000_000,
        this.currentPage(),
        this.pageSize(),
        this.selectedSort() || 'price,asc',
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
          this.pageNumbers.set(Array.from({ length: totalPages }, (_, i) => i));
          this.loading.set(false);
        },
        error: (err) => {
          if (requestId !== this.requestId) return;
          console.error('Failed to load products', err);
          this.loading.set(false);
          // Used to look exactly like "no products found"; a failing server is not an empty shop.
          this.loadError.set(true);
        },
      });
  }

  // ---------- Event handlers ----------
  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value || '';
    this.searchInput.set(value);

    // One request after the user stops typing, instead of one per keystroke.
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

  // ---------- Wishlist ----------
  isInWishlist(productId: number): boolean {
    return this.wishlistService.isWishlisted(productId);
  }

  toggleWishlist(productId: number): void {
    if (!this.authService.isLoggedIn()) {
      this.toastService.error('Please login first to use your wishlist.');
      return;
    }
    this.wishlistService.toggle(productId).subscribe({
      next: (res) => {
        this.toastService.success(res.data ? 'Added to wishlist.' : 'Removed from wishlist.');
      },
      error: (err) => this.toastService.error(err.error?.message || 'Could not update wishlist.'),
    });
  }

  // ---------- Pricing ----------
  discountedPrice(product: ProductResponse): number {
    if (!product.discountPercentage) return product.price;
    return product.price * (1 - product.discountPercentage / 100);
  }

  // ---------- Cart Logic ----------
  addToCart(productId: number): void {
    // Guests: send them to sign in and bring them back here (the server would answer 401 anyway).
    if (!this.authService.isLoggedIn()) {
      this.toastService.info('Please sign in to add items to your cart.');
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

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
  }

  buyNow(productId: number): void {
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
  }
  // Removed (never used): recentlyViewed, selectedProduct, showQuickView, categoryCounts,
  // getCategoryCount (it counted only the 12 products on the CURRENT page, so the sidebar numbers
  // never matched the Categories page; the sidebar now uses the server's cat.productCount),
  // onCategoryChange, trackByIndex.
}