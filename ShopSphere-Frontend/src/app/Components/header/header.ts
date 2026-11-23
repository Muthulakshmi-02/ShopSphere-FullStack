import { Component, inject, computed, signal, ElementRef, HostListener, OnDestroy } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../Services/auth-service';
import { CartService } from '../../Services/cart-service';
import { ProductService } from '../../Services/product-service';
import { ProductResponse } from '../../Models/Product.model';
import { productImageUrl } from '../../shared/api.config';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Subscription, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, MatIconModule, FormsModule, CurrencyPipe],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class Header implements OnDestroy {
  public auth = inject(AuthService);
  public cartService = inject(CartService);
  private productService = inject(ProductService);
  private router = inject(Router);
  private elementRef = inject(ElementRef);

  protected readonly productImageUrl = productImageUrl;

  // Mobile menu state
  public isMobileMenuOpen = signal(false);

  // Live search state
  public searchQuery = signal('');
  public searchSuggestions = signal<ProductResponse[]>([]);
  public isSearching = signal(false);
  public showSuggestions = signal(false);

  private searchSubject = new Subject<string>();
  private searchSubscription: Subscription;

  constructor() {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      switchMap(query => {
        const q = query?.trim();
        if (!q || q.length < 2) {
          this.isSearching.set(false);
          return of(null);
        }
        this.isSearching.set(true);
        return this.productService.getFilteredProducts(q, null, 0, 1_000_000, 0, 5, 'price,asc');
      })
    ).subscribe({
      next: (res) => {
        this.isSearching.set(false);
        if (res?.data?.content) {
          this.searchSuggestions.set(res.data.content);
          this.showSuggestions.set(true);
        } else {
          this.searchSuggestions.set([]);
        }
      },
      error: () => {
        this.isSearching.set(false);
        this.searchSuggestions.set([]);
      }
    });
  }

  ngOnDestroy() {
    this.searchSubscription.unsubscribe();
  }

  cartCount = computed(() => {
    const data = this.cartService.cartData();
    if (!data || !data.items) return 0;
    return data.items.reduce((acc: number, item: any) => acc + (item.quantity || 0), 0);
  });

  isUserLoggedIn = computed(() => !!this.auth.currentUser());

  // Role detection
  isAdmin = computed(() => {
    const role = (this.auth.currentUser()?.role ?? '').toString().toUpperCase();
    return role === 'ADMIN' || role === 'ADMINISTRATOR';
  });

  isCustomer = computed(() => this.isUserLoggedIn() && !this.isAdmin());

  onSearchInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    if (!value || value.trim().length < 2) {
      this.searchSuggestions.set([]);
      this.showSuggestions.set(false);
    }
    this.searchSubject.next(value);
  }

  onSearchFocus() {
    if (this.searchSuggestions().length > 0) {
      this.showSuggestions.set(true);
    }
  }

  onSearchSubmit() {
    const query = this.searchQuery().trim();
    this.showSuggestions.set(false);
    if (query) {
      this.router.navigate(['/products'], { queryParams: { search: query } });
      this.closeMobileMenu();
    }
  }

  onSelectProduct(product: ProductResponse) {
    this.showSuggestions.set(false);
    this.searchQuery.set(product.name);
    this.router.navigate(['/products', product.productId]);
    this.closeMobileMenu();
  }

  clearSearch() {
    this.searchQuery.set('');
    this.searchSuggestions.set([]);
    this.showSuggestions.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.showSuggestions.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.showSuggestions.set(false);
  }

  goToOrders() {
    this.router.navigate(['/my-orders']);
    this.closeMobileMenu();
  }

  goToCart() {
    this.router.navigate(['/cart']);
    this.closeMobileMenu();
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
    this.closeMobileMenu();
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen.update(open => !open);
  }

  closeMobileMenu() {
    this.isMobileMenuOpen.set(false);
  }
}
