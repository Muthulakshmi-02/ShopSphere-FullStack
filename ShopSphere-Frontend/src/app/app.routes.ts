import { Routes } from '@angular/router';
import { adminGuard } from './Guards/admin-guard';
import { authGuard } from './Guards/auth-guard';
import { AdminDashboard } from './Components/admin-dashboard/admin-dashboard';
import { Login } from './Components/login/login';
import { ProductDashboard } from './Components/product-dashboard/product-dashboard';
import { Register } from './Components/register/register';
import { Cart } from './Components/cart/cart';
import { CheckoutPage } from './Components/checkout/checkout';
import { OrderSuccess } from './Components/order-succes/order-succes';
import { MyOrdersPage } from './Components/my-orders-page/my-orders-page';
import { ProductDetail } from './Components/product-detail/product-detail';
import { TermsOfService } from './Components/terms-of-service/terms-of-service';
import { PrivacyPolicy } from './Components/privacy-policy/privacy-policy';
import { WishlistPage } from './Components/wishlist-page/wishlist-page';
import { AdminLayout } from './layouts/admin-layout/admin-layout';
import { CategoriesPage } from './Components/categories-page/categories-page';

export const routes: Routes = [
  // Browsing is public (GET /api/products/** is permitAll on the backend).
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'products', component: ProductDashboard },
  { path: 'products/:id', component: ProductDetail },
  { path: 'terms', component: TermsOfService },
  { path: 'privacy', component: PrivacyPolicy },
  { path: 'categories', component: CategoriesPage },

  // Pages that only make sense when logged in. authGuard (it already existed but was never
  // used) sends guests to /login and returns them here afterwards. Before, a guest could open
  // these pages and just got a wall of failed requests.
  { path: 'cart', component: Cart, canActivate: [authGuard] },
  { path: 'wishlist', component: WishlistPage, canActivate: [authGuard] },
  { path: 'checkout', component: CheckoutPage, canActivate: [authGuard] },
  { path: 'order-success', component: OrderSuccess, canActivate: [authGuard] },
  { path: 'my-orders', component: MyOrdersPage, canActivate: [authGuard] },

  {
    path: 'admindashboard',
    component: AdminLayout,
    canActivate: [adminGuard],
    children: [{ path: '', component: AdminDashboard }],
  },

  // Fallback
  { path: '**', redirectTo: '' },
];