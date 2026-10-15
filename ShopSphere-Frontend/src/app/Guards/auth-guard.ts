import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../Services/auth-service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  // The login state lives in the BROWSER's storage, which the server (SSR) cannot see. Without
  // this line a logged-in user refreshing /my-orders was redirected by the server render.
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  if (authService.isLoggedIn()) {
    return true;
  }

  // Send them to login and bring them back to the page they wanted afterwards.
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};