import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // SSR SAFETY FIX: sessionStorage doesn't exist on the server (main.server.ts
  // renders this app with Angular Universal), so guard the access or every
  // SSR request would throw a ReferenceError.
  const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('token') : null;

  // Check if token exists and request is going to your backend
  if (token && req.url.includes('localhost:8080')) {

    // Clean the token: remove "Bearer " if it's already there to avoid "Bearer Bearer"
    const cleanToken = token.replace('Bearer ', '');

    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${cleanToken}` // Notice the SPACE after Bearer
      }
    });
    return next(cloned);
  }

  return next(req);
};