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

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  // The same rules the server enforces now (RegisterUserDto), so the user hears about a problem
  // while typing instead of after submitting. No role field: the server always assigns USER.
  registerForm = this.fb.nonNullable.group({
    userName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.maxLength(72),
      Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)
    ]]
  });

  // The specific message for a field, or null while it is untouched / valid.
  fieldError(name: 'userName' | 'email' | 'password'): string | null {
    const c = this.registerForm.get(name);
    if (!c || !c.touched || !c.errors) return null;

    if (name === 'userName') {
      if (c.errors['required']) return 'Name is required';
      return 'Name must be 3 to 30 characters';
    }
    if (name === 'email') {
      return 'Valid email required';
    }
    if (c.errors['required']) return 'Password is required';
    if (c.errors['minlength'] || c.errors['maxlength']) return 'Password must be 8 to 72 characters';
    return 'Password needs at least one letter and one number';
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.register(this.registerForm.getRawValue()).subscribe({
      next: (res) => {
        if (res.success) {
          // The login page shows "Account created. Please sign in."
          this.router.navigate(['/login'], { queryParams: { registered: '1' } });
        } else {
          this.errorMessage.set(res.message || 'Registration failed. Try a different email.');
          this.isLoading.set(false);
        }
      },
      error: (err) => {
        this.errorMessage.set(
          err.status === 0
            ? "Can't reach the server. Please try again."
            : (err.error?.message || 'Could not complete registration. Please try again.')
        );
        this.isLoading.set(false);
      }
    });
  }

  togglePassword() {
    this.showPassword.update(show => !show);
  }
}