import { Component, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../Services/auth-service';
import { CartService } from '../../Services/cart-service';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, MatIconModule],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class Header {
  public auth = inject(AuthService);
  public cartService = inject(CartService);
  private router = inject(Router);

  // Mobile menu state
  public isMobileMenuOpen = signal(false);

  cartCount = computed(() => {
    const data = this.cartService.cartData();
    if (!data || !data.items) return 0;
    return data.items.reduce((acc: number, item: any) => acc + (item.quantity || 0), 0);
  });

  isUserLoggedIn = computed(() => !!this.auth.currentUser());

  // CHANGED: the server sends the role as "ADMIN" / "USER" (the enum name). The old check compared
  // with 'Admin' / 'Administrator', which never matched, so an admin was treated as a customer.
  isAdmin = computed(() => {
    const role = (this.auth.currentUser()?.role ?? '').toString().toUpperCase();
    return role === 'ADMIN' || role === 'ADMINISTRATOR';
  });

  // CHANGED: this used to be "not an admin", which also counted people who are NOT LOGGED IN. That is
  // why a signed-out visitor saw "My Orders" and the wishlist heart in the header.
  isCustomer = computed(() => this.isUserLoggedIn() && !this.isAdmin());

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

  // Removed: userInitials (the template uses auth.userInitials()), and the unused search and
  // cart-drawer code (searchQuery, onSearch, onSearchInput, onSearchKeydown, onCartClick):
  // nothing in the template calls them.
}
