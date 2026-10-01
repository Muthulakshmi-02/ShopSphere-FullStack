import { Component, OnInit, signal, inject, effect, computed } from '@angular/core';
import { ProductService, Page } from '../../Services/product-service';
import { CategoryService } from '../../Services/category-service';
import { CartService } from '../../Services/cart-service';
import { ProductResponse } from '../../Models/Product.model';
import { CategoryResponse } from '../../Models/Category.model';
import { Router, RouterLink } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import {
  CommonModule,
  CurrencyPipe,
  NgFor,
  NgIf,
} from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../shared/toast/toast-service';
import { WishlistService } from '../../Services/wishlist-service';
import { AuthService } from '../../Services/auth-service';

export interface SortOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-product-dashboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, FormsModule, NgFor, RouterLink],
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
  // ---------- State ----------
  products = signal<ProductResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  totalProducts = signal<number>(0);
  totalPages = signal<number>(0);
  loading = signal<boolean>(false);
  pageNumbers = signal<number[]>([]);
  readonly API_URL = 'http://localhost:8080';

  // ---------- Filters & pagination ----------
  searchKeyword = signal<string>('');
  selectedCategory = signal<number | null>(null);
  selectedSort = signal<string>('price,asc');
  currentPage = signal<number>(0);
  pageSize = signal<number>(12);
  categoryCounts = signal<Record<number, number>>({});
  recentlyViewed = signal<ProductResponse[]>([]);
  selectedProduct = signal<ProductResponse | null>(null);

  showQuickView = signal(false);
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
        this.currentPage();
        this.syncFiltersToUrl();
      },
      { allowSignalWrites: true },
    );
  }

  // ngOnInit() {
  //   this.loadCategories();
  //   this.loadProducts();

  //   // Wishlist is a per-user feature (backend requires auth) - only fetch
  //   // it if someone's actually logged in, otherwise leave hearts unfilled.
  //   if (this.authService.isLoggedIn()) {
  //     this.wishlistService.loadWishlistIds().subscribe({
  //       error: (err) => console.error('Failed to load wishlist', err),
  //     });
  //   }
  // }
  ngOnInit() {

    this.loadCategories();

    this.route.queryParams.subscribe(params => {

      this.searchKeyword.set(
        params['search'] || ''
      );

      this.selectedCategory.set(

        params['category']
          ? Number(params['category'])
          : null

      );

      this.selectedSort.set(
        params['sort'] || 'price,asc'
      );

    });

  }

  loadCategories() {
    this.categoryService.getAllCategories().subscribe({
      next: (res) => this.categories.set(res.data || []),
      error: (err) => console.error('Failed to load categories', err),
    });
  }

  loadProducts() {
    this.loading.set(true);
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

          const page = (res.data as Page<ProductResponse>) || {
            content: [],
            totalElements: 0,
            totalPages: 1
          };

          const content = page.content || [];

          const totalElements =
            page.totalElements ?? content.length;

          const totalPages =
            page.totalPages ?? 1;

          this.products.set(content);

          // ✅ Category Counts
          const counts: Record<number, number> = {};

          content.forEach(product => {

            counts[product.categoryId] =
              (counts[product.categoryId] || 0) + 1;

          });

          this.categoryCounts.set(counts);

          this.totalProducts.set(totalElements);

          this.totalPages.set(totalPages);
      

          this.pageNumbers.set(
            Array.from(
              { length: totalPages },
              (_, i) => i
            )
          );

          this.loading.set(false);

        },
        error: (err) => {
          console.error('Failed to load products', err);
          this.loading.set(false);
        },
      });
  }

  // ---------- Type-Safe Event Handlers ----------
  onSearchInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchKeyword.set(target.value || '');
    this.currentPage.set(0);
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
      error: () => this.toastService.error('Could not update wishlist.'),
    });
  }

  // ---------- Pricing ----------
  discountedPrice(product: ProductResponse): number {
    if (!product.discountPercentage) return product.price;
    return product.price * (1 - product.discountPercentage / 100);
  }

  // ---------- Cart Logic ----------
  addToCart(productId: number): void {
    const product = this.products().find(p => p.productId === productId);
    if (product?.variants && product.variants.length > 0) {
      // Can't pick a size/color from the listing grid - send them to the
      // product page instead of letting this fail with a backend error.
      this.toastService.info('This product has options - pick one on the product page.');
      this.router.navigate(['/products', productId]);
      return;
    }

    this.cartService.addItemToCart(productId, 1).subscribe({
      next: (res) => {
        this.toastService.success('Item added to your cart!');
        console.log('Item added:', res);
      },
      error: (err) => {
        console.error("Add to cart failed", err);
        if (err.status === 401) {
          this.toastService.error('Please login first to add items to the cart.');
        } else {
          this.toastService.error(err.error?.message || 'Could not add item to cart.');
        }
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

    // Pass product details directly to checkout via route state or query params
    this.router.navigate(['/checkout'], {
      queryParams: {
        buyNow: productId,
        quantity: 1
      }
    });
  }
  // ---------- Paging & Helpers ----------
  clearFilters(): void {
    this.searchKeyword.set('');
    this.selectedCategory.set(null);
    this.selectedSort.set('price,asc');
    this.currentPage.set(0);
  }

  onSortChange(sortValue: string): void {
    this.selectedSort.set(sortValue);
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
  // ---------- Type-Safe Event Handlers ----------

  onCategoryChange(event: Event): void {  // ← NEW METHOD
    const target = event.target as HTMLSelectElement;
    const value = target.value ? +target.value : null;
    this.selectedCategory.set(value);
    this.currentPage.set(0);
  }
  trackByIndex(index: number): number {
    return index;
  }
  private syncFiltersToUrl(): void {

    this.router.navigate(
      [],
      {
        relativeTo: this.route,

        queryParams: {

          search:
            this.searchKeyword() || null,

          category:
            this.selectedCategory(),

          sort:
            this.selectedSort(),

        },

        queryParamsHandling: '',

        replaceUrl: true
      }
    );

  }
  getCategoryCount(categoryId: number): number {

    return this.products()
      .filter(p => p.categoryId === categoryId)
      .length;

  }

}
