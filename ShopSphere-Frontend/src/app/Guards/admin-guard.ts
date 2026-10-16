import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../Services/auth-service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  // The login state lives in the BROWSER's storage, which the server (SSR) cannot see. Without this
  // line, an admin refreshing /admindashboard was redirected to /login by the server render first.
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  // The signal from the service is the right thing to check (not localStorage, which anyone can edit).
  if (authService.isAdmin()) {
    return true;
  }

  // Logged in as a normal customer: send them to the shop, not to a login page they don't need.
  if (authService.isLoggedIn()) {
    return router.createUrlTree(['/products']);
  }

  // Not logged in: go to login and come back here afterwards.
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};