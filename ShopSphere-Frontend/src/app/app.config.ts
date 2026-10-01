// import { ApplicationConfig } from '@angular/core';
// import { provideRouter } from '@angular/router';
// import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
// import { provideNoopAnimations } from '@angular/platform-browser/animations'; // Use Noop if pkg is missing
// import { routes } from './app.routes';
// import { authInterceptor } from './Interceptor/authInterceptor';

// export const appConfig: ApplicationConfig = {
//   providers: [
//     provideRouter(routes),
//     provideHttpClient(
//       withFetch(),
//       withInterceptors([authInterceptor])
//     ),
//     // This acts as a placeholder so the app doesn't crash
//   ]
// };

import { ApplicationConfig, LOCALE_ID, DEFAULT_CURRENCY_CODE } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { authInterceptor } from './Interceptor/authInterceptor';

// Import locale data for Indian English (INR)
import { registerLocaleData } from '@angular/common';
import localeIn from '@angular/common/locales/en-IN';

// Register the locale data
registerLocaleData(localeIn);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor])
    ),
    // Set default application locale to Indian English
    { provide: LOCALE_ID, useValue: 'en-IN' },
    // Force default currency code to INR (₹)
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'INR' }
  ]
};