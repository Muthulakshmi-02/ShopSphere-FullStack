import { Routes } from '@angular/router';
import { adminGuard } from './Guards/admin-guard';
import { authGuard } from './Guards/auth-guard';

export const routes: Routes = [
  // Browsing is public (GET /api/products/** is permitAll on the backend).
  { path: '', redirectTo: 'products', pathMatch: 'full' },

  // Auth pages — lightweight, load on demand.
  { path: 'login', loadComponent: () => import('./Components/login/login').then(m => m.Login) },
  { path: 'register', loadComponent: () => import('./Components/register/register').then(m => m.Register) },
  { path: 'forgot-password', loadComponent: () => import('./Components/login/ForgetPassword').then(m => m.ForgotPassword) },
  { path: 'reset-password', loadComponent: () => import('./Components/login/ResetPassword').then(m => m.ResetPassword) },

  // Public catalog pages.
  { path: 'products', loadComponent: () => import('./Components/product-dashboard/product-dashboard').then(m => m.ProductDashboard) },
  { path: 'products/:id', loadComponent: () => import('./Components/product-detail/product-detail').then(m => m.ProductDetail) },
  { path: 'categories', loadComponent: () => import('./Components/categories-page/categories-page').then(m => m.CategoriesPage) },

  // Static / legal pages.
  { path: 'terms', loadComponent: () => import('./Components/terms-of-service/terms-of-service').then(m => m.TermsOfService) },
  { path: 'privacy', loadComponent: () => import('./Components/privacy-policy/privacy-policy').then(m => m.PrivacyPolicy) },

  // Pages that only make sense when logged in. authGuard sends guests to /login and returns them
  // here afterwards.
  { path: 'cart', loadComponent: () => import('./Components/cart/cart').then(m => m.Cart), canActivate: [authGuard] },
  { path: 'wishlist', loadComponent: () => import('./Components/wishlist-page/wishlist-page').then(m => m.WishlistPage), canActivate: [authGuard] },
  { path: 'checkout', loadComponent: () => import('./Components/checkout/checkout').then(m => m.CheckoutPage), canActivate: [authGuard] },
  { path: 'order-success', loadComponent: () => import('./Components/order-succes/order-succes').then(m => m.OrderSuccess), canActivate: [authGuard] },
  { path: 'my-orders', loadComponent: () => import('./Components/my-orders-page/my-orders-page').then(m => m.MyOrdersPage), canActivate: [authGuard] },
  { path: 'profile', loadComponent: () => import('./Components/profile/profile').then(m => m.ProfilePage), canActivate: [authGuard] },

  {
    path: 'admindashboard',
    loadComponent: () => import('./layouts/admin-layout/admin-layout').then(m => m.AdminLayout),
    canActivate: [adminGuard],
    children: [{
      path: '',
      loadComponent: () => import('./Components/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard),
    }],
  },

  // Fallback
  { path: '**', redirectTo: '' },
];
