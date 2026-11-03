import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginUserDto, LoginResponseDTO, ApiResponse, RegisterUserDto, UserResponseDTO } from '../Models/User.model';
import { CartService } from './cart-service';
import { API_BASE_URL } from '../shared/api.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  private cartService = inject(CartService);

  // CHANGED: was 'http://localhost:8080/api/auth'. It must follow API_BASE_URL like the other
  // services, or login would keep calling localhost after you deploy.
  private readonly API_URL = `${API_BASE_URL}/api/auth`;

  // The only two keys this service ever stores.
  private readonly TOKEN_KEY = 'token';
  private readonly USER_KEY = 'user';

  // The single source of truth for the user
  currentUser = signal<LoginResponseDTO | null>(null);

  isLoggedIn = computed(() => !!this.currentUser());

  isAdmin = computed(() => {
    const role = this.currentUser()?.role;
    return role?.toUpperCase() === 'ADMIN';
  });

  userInitials = computed(() => {
    const name = this.currentUser()?.userName;
    if (!name) return '👤';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  });

  constructor() {
    // Restore the session when the app loads.
    if (isPlatformBrowser(this.platformId)) {
      const token = sessionStorage.getItem(this.TOKEN_KEY);
      const savedUser = sessionStorage.getItem(this.USER_KEY);

      // A saved user without a valid token (missing or expired) must not look "logged in".
      // Cleared only when a saved user exists, so a first-time visitor's storage isn't touched.
      if (savedUser && (!token || this.isTokenExpired(token))) {
        this.clearStorage();
        return;
      }

      if (savedUser) {
        try {
          this.currentUser.set(JSON.parse(savedUser));
        } catch {
          this.clearStorage();
          return;
        }

        // NEW: after a refresh the user is restored but nothing loaded their cart, so the header's
        // cart badge showed 0 until the customer opened the cart page. (The wishlist hearts are
        // already restored by app.ts, so they are not touched here.)
        // Deferred one tick so this service is fully built before it makes requests.
        Promise.resolve().then(() => this.loadCustomerData());
      }
    }
  }

  // Stores the session. Navigation is the caller's job (the Login page), so it can send the
  // user to the page they originally wanted (returnUrl).
  login(credentials: LoginUserDto): Observable<ApiResponse<LoginResponseDTO>> {
    return this.http.post<ApiResponse<LoginResponseDTO>>(`${this.API_URL}/login`, credentials).pipe(
      tap(res => {
        if (res.success && res.data) {
          const userData = res.data;

          if (isPlatformBrowser(this.platformId)) {
            sessionStorage.setItem(this.TOKEN_KEY, userData.token);
            sessionStorage.setItem(this.USER_KEY, JSON.stringify(userData));
          }

          this.currentUser.set(userData);
          this.loadCustomerData();
        }
      })
    );
  }

  register(userData: RegisterUserDto): Observable<ApiResponse<UserResponseDTO>> {
    return this.http.post<ApiResponse<UserResponseDTO>>(`${this.API_URL}/register`, userData);
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']).then(() => {
      // Reload so no old user's data stays in memory.
      window.location.reload();
    });
  }

  // For the HTTP interceptor: call this when the server answers 401 to a request that carried a
  // token (expired or invalid session). No reload; the login page explains what happened.
  // Calling it several times in a row is safe: after the first call there is no user left.
  sessionExpired(): void {
    if (!this.currentUser()) return;
    this.clearSession();
    this.router.navigate(['/login'], { queryParams: { expired: '1' } });
  }

  // A cart belongs to customers only (the server refuses it for admins).
  private loadCustomerData(): void {
    if (!this.currentUser() || this.isAdmin()) return;
    this.cartService.loadCart().subscribe({ error: () => { } });
  }

  private clearSession(): void {
    this.clearStorage();
    this.currentUser.set(null);
    this.cartService.cartData.set(null);
    // (The wishlist is cleared by the effect in app.ts as soon as currentUser becomes null.)
  }

  // CHANGED: this used to call sessionStorage.clear() AND localStorage.clear(), which erases
  // everything the site has stored (other settings, other features), not just the login. It now
  // removes only the two keys this service owns, in both stores (the old version of the app may
  // have left a login in localStorage).
  private clearStorage(): void {
    if (isPlatformBrowser(this.platformId)) {
      for (const store of [sessionStorage, localStorage]) {
        store.removeItem(this.TOKEN_KEY);
        store.removeItem(this.USER_KEY);
      }
    }
  }

  // Reads the "exp" claim from the JWT (no secret needed to read it; the SERVER still verifies it).
  private isTokenExpired(token: string): boolean {
    try {
      const payload = token.split('.')[1];
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      const exp = JSON.parse(json).exp;
      return typeof exp === 'number' && exp * 1000 <= Date.now();
    } catch {
      return true; // unreadable token = treat as invalid
    }
  }
}