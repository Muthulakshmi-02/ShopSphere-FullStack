import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../Services/auth-service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // UI State Signals
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  // Registration Form Definition
  // NOTE: no role field here on purpose. Public self-registration always
  // creates a USER account; the backend also enforces this independently.
  registerForm = this.fb.nonNullable.group({
    userName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    // Call AuthService register
    this.authService.register(this.registerForm.getRawValue()).subscribe({
      next: (res) => {
        if (res.success) {
          console.log('✅ Registration successful, redirecting to login...');
          this.router.navigate(['/login']);
        } else {
          // Handle cases where the server returns success: false
          this.errorMessage.set(res.message || 'Registration failed. Try a different email.');
          this.isLoading.set(false);
        }
      },
      error: (err) => {
        console.error('❌ Registration Error:', err);
        // Display specific error (e.g., "Email already exists") from backend
        this.errorMessage.set(err.error?.message || 'Could not complete registration. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  togglePassword() {
    this.showPassword.update(show => !show);
  }
}