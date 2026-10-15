import { Component, signal, inject, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../Services/auth-service';

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
  private route = inject(ActivatedRoute);

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);
  infoMessage = signal<string | null>(null);

  // Password: only "required" here. The old minLength(8) locked out anyone whose account was
  // created before password rules existed. Strength rules belong on registration.
  loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  ngOnInit() {
    const params = this.route.snapshot.queryParamMap;
    if (params.get('registered') === '1') {
      this.infoMessage.set('Account created. Please sign in.');
    } else if (params.get('expired') === '1') {
      this.infoMessage.set('Your session expired. Please sign in again.');
    }

    if (this.authService.isLoggedIn()) {
      this.router.navigateByUrl(this.destination());
    }
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.infoMessage.set(null);

    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success) {
          this.router.navigateByUrl(this.destination());
        } else {
          this.errorMessage.set(res.message || 'Invalid email or password.');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 0) {
          // Before, a stopped backend showed "Invalid email or password", which was misleading.
          this.errorMessage.set("Can't reach the server. Please check your connection and try again.");
        } else {
          this.errorMessage.set(err.error?.message || 'Invalid email or password. Please try again.');
        }
      }
    });
  }

  togglePasswordVisibility() {
    this.showPassword.update(show => !show);
  }

  // Admins always go to the dashboard. Everyone else goes back to the page they came from
  // (?returnUrl=...), or the shop. Only same-site paths are accepted, so a crafted link cannot
  // bounce someone to another website after login (open redirect).
  private destination(): string {
    if (this.authService.isAdmin()) return '/admindashboard';

    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const safe = !!returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') && !returnUrl.includes('://');
    return safe ? returnUrl! : '/products';
  }
}