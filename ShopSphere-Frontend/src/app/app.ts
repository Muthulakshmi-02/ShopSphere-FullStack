import { Component } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NgIf } from '@angular/common';
import { Header } from './Components/header/header';
import { CartService } from './Services/cart-service';
import { inject, signal } from '@angular/core';
import { AuthService } from './Services/auth-service';
import { OnInit } from '@angular/core';
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

  <div class="page-body">
    <router-outlet />
  </div>

  <app-footer *ngIf="!isAdminRoute()" />

  <app-toast-container />
  <app-confirm-modal />
`
})
export class App implements OnInit {
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private router = inject(Router);

  // The live sales ticker is a storefront device - it doesn't make sense
  // on admin screens (an admin doesn't need a marketing ticker while
  // managing inventory) or on login/register (nothing to browse yet).
  private readonly HIDDEN_ON = ['/admindashboard', '/login', '/register'];
  showTicker = signal(true);

  isAdminRoute = signal(false);


  ngOnInit() {
    // Only load the cart if a user session actually exists
    if (this.authService.isLoggedIn()) {
      this.cartService.loadCart();
    }

    this.updateTickerVisibility(this.router.url);
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(e => this.updateTickerVisibility(e.urlAfterRedirects));
  }

  private updateTickerVisibility(url: string): void {

    this.showTicker.set(
      !this.HIDDEN_ON.some(path => url.startsWith(path))
    );

    this.isAdminRoute.set(
      url.startsWith('/admindashboard')
    );
  }
}