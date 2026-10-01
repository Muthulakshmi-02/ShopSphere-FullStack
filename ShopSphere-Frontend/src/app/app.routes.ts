import { Routes } from '@angular/router';
import { adminGuard } from './Guards/admin-guard';
import { AdminDashboard } from './Components/admin-dashboard/admin-dashboard';
import { Login } from './Components/login/login';
import { ProductDashboard } from './Components/product-dashboard/product-dashboard';
import { Register } from './Components/register/register';
import { Cart } from './Components/cart/cart';
// Import your new components
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
  // Browsing products is public on the backend (GET /api/products/** is
  // permitAll()) - the homepage should reflect that, not force a login wall.
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'products', component: ProductDashboard },
  { path: 'products/:id', component: ProductDetail },
  { path: 'terms', component: TermsOfService },
  { path: 'privacy', component: PrivacyPolicy },
  { path: 'cart', component: Cart },
  { path: 'wishlist', component: WishlistPage },
  {
  path: 'categories',
  component: CategoriesPage
},

  // --- NEW PURCHASE FLOW ROUTES ---
  {
    path: 'checkout',
    component: CheckoutPage
    // canActivate: [userGuard] // Add this later to ensure only logged-in users buy
  },
  {
    path: 'order-success',
    component: OrderSuccess
  },
  {
    path: 'my-orders',
    component: MyOrdersPage
  },
  {
    path: 'admindashboard',
    component: AdminLayout,
    canActivate: [adminGuard],
    children: [
      {
        path: '',
        component: AdminDashboard
      }
    ]
  },

  // Fallback
  { path: '**', redirectTo: '' }
];