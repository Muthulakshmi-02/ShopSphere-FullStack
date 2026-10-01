import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../Services/auth-service';
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Use the signal from the service - it's reactive and faster than storage
  if (authService.isAdmin()) {
    return true;
  }

  console.warn('Access denied to admin dashboard');
  return router.parseUrl('/login');
};