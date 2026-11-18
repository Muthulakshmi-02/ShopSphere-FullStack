import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../shared/toast/toast-service';

/**
 * Global HTTP Error Interceptor
 * Catches network failures, 403 Forbidden, 429 Rate Limits, and 5xx Server Errors,
 * surfacing clear user-facing toast notifications.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // Network disconnect or backend completely unreachable
      if (err.status === 0) {
        toast.error('Unable to connect to the server. Please check your network connection.');
      } else if (err.status === 403) {
        const msg = err.error?.message || 'Access denied. You do not have permission to perform this action.';
        toast.error(msg);
      } else if (err.status === 429) {
        const msg = err.error?.message || 'Too many requests. Please wait a moment and try again.';
        toast.error(msg);
      } else if (err.status >= 500) {
        const msg = err.error?.message || 'Server error occurred. Please try again later.';
        toast.error(msg);
      }

      // Pass error down to component observers for local handling if needed
      return throwError(() => err);
    })
  );
};
