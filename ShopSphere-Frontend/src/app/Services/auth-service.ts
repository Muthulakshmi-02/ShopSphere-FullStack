import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginUserDto, LoginResponseDTO, ApiResponse, RegisterUserDto } from '../Models/User.model';
import { CartService } from './cart-service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  private cartService = inject(CartService);

  private readonly API_URL = 'http://localhost:8080/api/auth';

  // State: The single source of truth for the user
  currentUser = signal<LoginResponseDTO | null>(null);

  // Derived States: Calculated automatically when currentUser changes
  isLoggedIn = computed(() => !!this.currentUser());

  // Case-insensitive role check for Admin
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
    // Hydrate state from storage on app load
    if (isPlatformBrowser(this.platformId)) {
      const savedUser = sessionStorage.getItem('user');
      if (savedUser) {
        try {
          const user = JSON.parse(savedUser);
          this.currentUser.set(user);
        } catch (e) {
          console.error("Failed to parse saved user", e);
          this.logout();
        }
      }
    }
  }

  login(credentials: LoginUserDto): Observable<ApiResponse<LoginResponseDTO>> {
    return this.http.post<ApiResponse<LoginResponseDTO>>(`${this.API_URL}/login`, credentials).pipe(
      tap(res => {
        if (res.success && res.data) {
          const userData = res.data;
          const token = userData.token;
          const role = userData.role.toUpperCase(); // Normalize role string

          if (isPlatformBrowser(this.platformId)) {
            // Store token and user data consistently
            sessionStorage.setItem('token', token);
            sessionStorage.setItem('user', JSON.stringify(userData));

            // Sync with localStorage for Guards
            localStorage.setItem('token', token);
            localStorage.setItem('role', role);
          }

          // Update the Signal state
          this.currentUser.set(userData);

          // REDIRECTION LOGIC
          if (role === 'ADMIN') {
            console.log('Redirecting to Admin Dashboard...');
            this.router.navigate(['/admindashboard']);
          } else {
            console.log('Redirecting to Products Page...');
            this.router.navigate(['/products']);

            // CRITICAL: Must .subscribe() to trigger the HTTP call
            this.cartService.loadCart().subscribe({
              next: () => console.log('User cart loaded'),
              error: (err) => console.error('Cart load failed', err)
            });
          }
        }
      })
    );
  }

  register(userData: RegisterUserDto): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.API_URL}/register`, userData);
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      // 1. Clear ALL storage
      sessionStorage.clear();
      localStorage.clear();
    }

    // 2. Reset Auth Signal
    this.currentUser.set(null);

    // 3. Reset Cart Data in the CartService
    this.cartService.cartData.set(null);

    // 4. Redirect to login
    this.router.navigate(['/login']).then(() => {
      // Small delay before reload to ensure router clears state
      window.location.reload();
    });
  }
}