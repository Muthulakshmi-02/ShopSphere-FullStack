import { Component, signal, inject, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../Services/auth-service';
import { CartService } from '../../Services/cart-service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class Login implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cartService = inject(CartService);

  // State Management
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  // Form with validation
  loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    // We pass the raw values to the AuthService
    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: (res) => {
        console.log('✅ Login successful');

        // The AuthService already handles:
        // 1. Setting the currentUser signal
        // 2. Saving the token to storage
        // 3. Navigating to the correct dashboard
        // 4. Triggering cartService.loadCart()

        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('❌ Login Error:', err);
        // Display specific error message from backend if available
        this.errorMessage.set(err.error?.message || 'Invalid email or password. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  togglePasswordVisibility() {
    this.showPassword.update(show => !show);
  }
  // inside login.ts
  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      if (this.authService.isAdmin()) {
        this.router.navigate(['/admindashboard']);
      } else {
        this.router.navigate(['/products']);
      }
    }
  }
}