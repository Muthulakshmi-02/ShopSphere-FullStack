import { Component, effect, inject, signal, untracked, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NgIf } from '@angular/common';
import { Header } from './Components/header/header';
import { AuthService } from './Services/auth-service';
import { WishlistService } from './Services/wishlist-service';
import { ToastContainer } from './shared/toast/toast-container';
import { ConfirmModal } from './shared/confirm-modal/confirm-modal';
import { Footer } from './Components/footer/footer';
import { LiveTicker } from './shared/live-ticker/live-ticker';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NgIf, Header, ToastContainer, ConfirmModal, Footer, LiveTicker],
  template: `
  <app-live-ticker *ngIf="showTicker()" />

  <app-header *ngIf="!isAdminRoute()" />

  <main class="page-body">
    <router-outlet />
  </main>

  <app-footer *ngIf="!isAdminRoute()" />

  <app-toast-container />
  <app-confirm-modal />
`
})
export class App implements OnInit {
  private authService = inject(AuthService);
  private wishlistService = inject(WishlistService);
  private router = inject(Router);

  // The live sales ticker is a storefront device. Hidden on admin screens, on login/register
  // (nothing to browse yet) and now also on the cart/checkout pages, where a scrolling
  // "someone just bought..." bar only distracts from paying.
  private readonly HIDDEN_ON = ['/admindashboard', '/login', '/register', '/checkout', '/order-success'];
  showTicker = signal(true);

  isAdminRoute = signal(false);

  constructor() {
    // Keeps the wishlist hearts correct on EVERY page for a logged-in customer: on app start
    // (page refresh) and right after login. Before, hearts were only loaded by the shop page, so
    // opening a product page directly showed an empty heart for a wishlisted product.
    effect(() => {
      const isCustomer = this.authService.isLoggedIn() && !this.authService.isAdmin();
      untracked(() => {
        if (isCustomer) {
          this.wishlistService.loadWishlistIds().subscribe({ error: () => { } });
        } else {
          this.wishlistService.clear();
        }
      });
    });
  }

  ngOnInit() {
    // The old "if (isLoggedIn) this.cartService.loadCart();" was removed: it never subscribed to
    // the request, so it did nothing at all. The cart is loaded in AuthService.login() and, after a
    // page refresh, in Header.ngOnInit().
    this.updateTickerVisibility(this.router.url);
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.updateTickerVisibility(e.urlAfterRedirects));
  }

  private updateTickerVisibility(url: string): void {
    this.showTicker.set(!this.HIDDEN_ON.some(path => url.startsWith(path)));
    this.isAdminRoute.set(url.startsWith('/admindashboard'));
  }
}