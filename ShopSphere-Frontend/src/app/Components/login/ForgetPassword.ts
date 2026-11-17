import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../Services/auth-service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './ForgetPassword.html',
  // Same look as the sign-in page.
  styleUrls: ['../login/login.css']
})
export class ForgotPassword {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  // Set once the request went through. The text comes from the server and is the same whether or
  // not the email has an account.
  sentMessage = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.forgotPassword(this.form.getRawValue().email.trim()).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.sentMessage.set(
          res.message ||
          "If an account exists for that email, we've sent a link to reset the password. It works for 30 minutes."
        );
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 0) {
          this.errorMessage.set("Can't reach the server. Please check your connection and try again.");
        } else {
          // 429 ("Too many requests...") and validation messages come from the server.
          this.errorMessage.set(err.error?.message || 'Could not send the email. Please try again.');
        }
      }
    });
  }
}