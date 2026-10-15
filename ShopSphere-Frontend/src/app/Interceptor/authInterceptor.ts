import { Injector, inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../Services/auth-service';

// Move to an environment file before deploying (the same value is used in several services).
const API_ORIGIN = 'http://localhost:8080';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const injector = inject(Injector);

  // sessionStorage does not exist during server-side rendering.
  const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('token') : null;

  // startsWith, not includes(): the old check matched "localhost:8080" ANYWHERE in the URL, so a
  // request to any other site with that text in its path or query string would have been sent
  // your login token.
  const isApiCall = req.url.startsWith(API_ORIGIN + '/');

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
      if (err.status === 401 && token && isApiCall && !req.url.includes('/api/auth/')) {
        injector.get(AuthService).sessionExpired();
      }
      return throwError(() => err);
    })
  );
};