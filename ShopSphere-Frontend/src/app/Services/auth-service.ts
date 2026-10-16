import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginUserDto, LoginResponseDTO, ApiResponse, RegisterUserDto, UserResponseDTO } from '../Models/User.model';
import { CartService } from './cart-service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  private cartService = inject(CartService);

  private readonly API_URL = 'http://localhost:8080/api/auth';

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
      const token = sessionStorage.getItem('token');
      const savedUser = sessionStorage.getItem('user');

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
        }
      }
    }
  }

  // Stores the session. Navigation is now the caller's job (the Login page), so it can send the
  // user to the page they originally wanted (returnUrl).
  login(credentials: LoginUserDto): Observable<ApiResponse<LoginResponseDTO>> {
    return this.http.post<ApiResponse<LoginResponseDTO>>(`${this.API_URL}/login`, credentials).pipe(
      tap(res => {
        if (res.success && res.data) {
          const userData = res.data;
          const role = userData.role.toUpperCase();

          if (isPlatformBrowser(this.platformId)) {
            sessionStorage.setItem('token', userData.token);
            sessionStorage.setItem('user', JSON.stringify(userData));
          }

          this.currentUser.set(userData);

          if (role !== 'ADMIN') {
            this.cartService.loadCart().subscribe({ error: () => { } });
          }
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
  sessionExpired(): void {
    if (!this.currentUser()) return;   // already logged out, nothing to do
    this.clearSession();
    this.router.navigate(['/login'], { queryParams: { expired: '1' } });
  }

  private clearSession(): void {
    this.clearStorage();
    this.currentUser.set(null);
    this.cartService.cartData.set(null);
  }

  private clearStorage(): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.clear();
      localStorage.clear();
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