import { Injector, inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../Services/auth-service';
import { API_BASE_URL } from '../shared/api.config';

// NEW: when a token expires, several requests are usually in flight at once (cart, orders, the
// header's counters...). Each of them gets its own 401, and each used to trigger the "session
// expired" flow: several redirects and several messages. Only the first one now does.
let expiryHandled = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const injector = inject(Injector);

  // sessionStorage does not exist during server-side rendering.
  const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('token') : null;

  // startsWith, not includes(): the token must never be sent to any other website.
  const isApiCall = req.url.startsWith(API_BASE_URL + '/');

  let request = req;
  if (token && isApiCall) {
    const cleanToken = token.replace(/^Bearer\s+/i, '');
    request = req.clone({ setHeaders: { Authorization: `Bearer ${cleanToken}` } });
  }

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      // 401 = the token is missing, expired or invalid. Send the user back to login with an
      // explanation. Login/register calls are excluded: a wrong password must show an error
      // on the form, not "session expired".
      if (err.status === 401 && token && isApiCall && !req.url.includes('/api/auth/') && !expiryHandled) {
        expiryHandled = true;
        injector.get(AuthService).sessionExpired();
        // Allow it again later (for example after the user signs in again).
        setTimeout(() => { expiryHandled = false; }, 3000);
      }
      return throwError(() => err);
    })
  );
};