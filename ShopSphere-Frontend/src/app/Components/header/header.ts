import { Component, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../Services/auth-service';
import { CartService } from '../../Services/cart-service';
import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive,MatIconModule],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class Header {
  // Services
  public auth = inject(AuthService);
  public cartService = inject(CartService);
  private router = inject(Router);

  // Reactive signals
  public searchQuery = signal('');

  // Mobile menu state
  public isMobileMenuOpen = signal(false);

  // Computed properties
  userInitials = computed(() => {
    const user = this.auth.currentUser();
    if (!user?.userName) return '👤';
    return user.userName
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  });

  cartCount = computed(() => {
    const data = this.cartService.cartData();
    if (!data || !data.items) return 0;
    return data.items.reduce((acc: number, item: any) => acc + (item.quantity || 0), 0);
  });

  isUserLoggedIn = computed(() => !!this.auth.currentUser());

  // Navigation methods
  goToOrders() {
    this.router.navigate(['/my-orders']);
    this.closeMobileMenu();
  }
  goToCart() {
    this.router.navigate(['/cart']);
    // Optionally close mobile menu if open
    this.closeMobileMenu();
  }

  onSearch() {
    const q = this.searchQuery().trim();
    if (!q) return;
    this.router.navigate(['/products'], {
      queryParams: { search: q },
    });
    this.searchQuery.set('');
  }

  onCartClick() {
    this.cartService.isCartOpen.set(true);
    this.closeMobileMenu();
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
    this.closeMobileMenu();
  }

  // Mobile menu toggle
  toggleMobileMenu() {
    this.isMobileMenuOpen.update(open => !open);
  }

  closeMobileMenu() {
    this.isMobileMenuOpen.set(false);
  }

  // Search input handler (for template reference)
  onSearchInput(event: Event) {
    const target = event.target as HTMLInputElement;
    this.searchQuery.set(target.value);
  }

  // Handle Enter key in search
  onSearchKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.onSearch();
    }
  }
}
