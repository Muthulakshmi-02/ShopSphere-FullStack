import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../Services/auth-service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './resetpassword.html',
  // Same look as the sign-in page.
  styleUrls: ['../login/login.css']
})
export class ResetPassword implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // The code from the emailed link (/reset-password?token=...).
  token = signal('');
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  // The same rules the server enforces (and the register page uses).
  form = this.fb.nonNullable.group({
    newPassword: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.maxLength(72),
      Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)
    ]],
    confirmPassword: ['', [Validators.required]]
  });

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    this.token.set(token);

    // Take the code out of the address bar once it has been read, so it does not stay in the browser
    // history, in a screenshot, or in the "referrer" sent to other sites.
    if (token) {
      this.router.navigate([], { replaceUrl: true, queryParams: {} });
    }
  }

  // The specific message for the new-password field, or null while it is untouched / valid.
  passwordError(): string | null {
    const c = this.form.get('newPassword');
    if (!c || !c.touched || !c.errors) return null;
    if (c.errors['required']) return 'Password is required';
    if (c.errors['minlength'] || c.errors['maxlength']) return 'Password must be 8 to 72 characters';
    return 'Password needs at least one letter and one number';
  }

  get mismatch(): boolean {
    const { newPassword, confirmPassword } = this.form.getRawValue();
    return !!this.form.get('confirmPassword')?.touched && confirmPassword !== newPassword;
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    const { newPassword, confirmPassword } = this.form.getRawValue();
    if (this.form.invalid || newPassword !== confirmPassword) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.resetPassword(this.token(), newPassword).subscribe({
      next: () => {
        this.isLoading.set(false);
        // The login page shows "Your password was updated. Please sign in."
        this.router.navigate(['/login'], { queryParams: { reset: '1' } });
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.status === 0
            ? "Can't reach the server. Please try again."
            : (err.error?.message || 'Could not update your password. Please request a new link.')
        );
      }
    });
  }

  togglePassword(): void {
    this.showPassword.update(show => !show);
  }
}