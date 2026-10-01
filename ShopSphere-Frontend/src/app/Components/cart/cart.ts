// import { Component, inject, OnInit } from '@angular/core';
// import { CommonModule, CurrencyPipe } from '@angular/common';
// import { RouterLink } from '@angular/router';
// import { CartService } from '../../Services/cart-service';
// import { signal } from '@angular/core';
// import { productImageUrl } from '../../shared/api.config';
// import { ToastService } from '../../shared/toast/toast-service';
// import { ConfirmService } from '../../shared/confirm-modal/confirm-service';

// @Component({
//   selector: 'app-cart-page',
//   standalone: true,
//   imports: [CommonModule, RouterLink, CurrencyPipe],
//   templateUrl: './cart.html',
//   styleUrls: ['./cart.css'],
// })
// export class Cart implements OnInit {
//   // Use 'public' so the HTML template can access the service directly if needed
//   public cartService = inject(CartService);
//   private toastService = inject(ToastService);
//   private confirmService = inject(ConfirmService);

//   // Expose the helper to the template so images resolve to the backend host
//   protected readonly productImageUrl = productImageUrl;

//   // Signals - These will auto-update the UI when the service signals change
//   // We use () in the template to read these
//   cart = this.cartService.cartData;
//   itemCount = this.cartService.itemCount;
//   cartItems = signal<any[]>([]);
//   loading = signal(false);
//   // True only during the very first cart fetch - drives the skeleton loader
//   initialLoading = signal(true);

//   /**
//    * ngOnInit runs as soon as the component loads.
//    * This ensures that if the user refreshes the page or navigates via the header,
//    * the most recent data is pulled from the Spring Boot backend.
//    */
//   ngOnInit(): void {
//     this.loadCartData();
//   }

//   loadCartData() {
//     this.cartService.loadCart().subscribe({
//       next: (response) => {
//         console.log('Cart synchronized with server:', response);
//         this.initialLoading.set(false);
//       },
//       error: (err) => {
//         console.error('Failed to load cart on init:', err);
//         this.initialLoading.set(false);
//         this.toastService.error('Could not load your cart. Please try again.');
//       }
//     });
//   }

//   updateQty(productId: number, currentQty: number, change: number, variantId?: number | null) {
//     const newQty = currentQty + change;

//     if (newQty < 1) {
//       this.remove(productId, variantId);
//     } else {
//       // .subscribe() is REQUIRED to trigger the HTTP request
//       this.cartService.updateQuantity(productId, newQty, variantId).subscribe({
//         next: () => console.log(`Updated quantity for product ${productId}`),
//         error: (err) => {
//           console.error(err);
//           this.toastService.error('Failed to update quantity. Check stock availability.');
//         }
//       });
//     }
//   }

//   remove(cartItemId: number, variantId?: number | null) {
//     // 1. Start loading to prevent double-clicks
//     this.loading.set(true);

//     this.cartService.removeItem(cartItemId, variantId).subscribe({
//       next: () => {
//         console.log(`Item ${cartItemId} removed from database`);

//         // 2. IMMEDIATELY filter the list so the item disappears from the screen
//         this.cartItems.update(items => items.filter(item => item.id !== cartItemId));

//         this.loading.set(false);
//       },
//       error: (err) => {
//         this.loading.set(false);
//         // If 404, it means it's already gone, so just remove it from UI anyway
//         if (err.status === 404) {
//           this.cartItems.update(items => items.filter(item => item.id !== cartItemId));
//         } else {
//           this.toastService.error('Could not remove item. Server error.');
//         }
//       }
//     });
//   }

//   async clearCart() {
//     const confirmed = await this.confirmService.ask({
//       title: 'Clear your cart?',
//       message: 'This will remove all items from your cart. This can\'t be undone.',
//       confirmLabel: 'Clear cart',
//       danger: true
//     });
//     if (!confirmed) return;

//     this.cartService.clearCart().subscribe({
//       next: (res) => {
//         if (res.success) {
//           this.cartItems.set([]);
//           this.toastService.success('Cart cleared.');
//         } else {
//           this.toastService.error(res.message || 'Failed to clear cart');
//         }
//       },
//       error: (err) => {
//         console.error('HTTP Error:', err);
//         this.toastService.error('Failed to clear cart: Server error.');
//       }
//     });
//   }
// }

import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../Services/cart-service';
import { productImageUrl } from '../../shared/api.config';
import { ToastService } from '../../shared/toast/toast-service';
import { ConfirmService } from '../../shared/confirm-modal/confirm-service';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CurrencyPipe, FormsModule],
  templateUrl: './cart.html',
  styleUrls: ['./cart.css'],
})
export class Cart implements OnInit {
  public cartService = inject(CartService);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmService);
  private router = inject(Router);

  protected readonly productImageUrl = productImageUrl;

  cart = this.cartService.cartData;
  itemCount = this.cartService.itemCount;
  cartItems = signal<any[]>([]);
  loading = signal(false);
  initialLoading = signal(true);

  // --- Promo Code State ---
  promoCode: string = '';
  promoMessage: string = '';
  isPromoApplied: boolean = false;
  discountRate: number = 0;
  flatDiscount: number = 0;

  ngOnInit(): void {
    this.loadCartData();
  }

  loadCartData() {
    this.cartService.loadCart().subscribe({
      next: (response) => {
        console.log('Cart synchronized with server:', response);
        this.initialLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load cart on init:', err);
        this.initialLoading.set(false);
        this.toastService.error('Could not load your cart. Please try again.');
      }
    });
  }

  // --- Calculated Order Totals ---
  get subtotal(): number {
    return this.cart()?.totalAmount || 0;
  }

  get discountAmount(): number {
    if (this.discountRate > 0) {
      return this.subtotal * this.discountRate;
    }
    return this.flatDiscount;
  }

  get finalGrandTotal(): number {
    const total = this.subtotal - this.discountAmount;
    return total > 0 ? total : 0;
  }

  // --- Promo Code Handlers (Connected to Backend) ---
  applyPromo() {
    const code = this.promoCode.trim().toUpperCase();

    if (!code) {
      this.promoMessage = 'Please enter a promo code.';
      this.isPromoApplied = false;
      this.resetDiscount();
      return;
    }

    this.cartService.validatePromoCode(code).subscribe({
      next: (res) => {
        const data = res.data;

        if (data?.used) {
          this.promoMessage = data.message || 'You have already used this promo code on a previous order.';
          this.isPromoApplied = false;
          this.resetDiscount();
        } else if (data?.valid) {
          this.isPromoApplied = true;

          if (data.type === 'percent' && data.value != null) {
            this.discountRate = data.value;
            this.flatDiscount = 0;
            this.promoMessage = `Code applied! ${data.value * 100}% discount added.`;
          } else if (data.value != null) {
            this.flatDiscount = data.value;
            this.discountRate = 0;
            this.promoMessage = `Code applied! ₹${data.value} discount added.`;
          }
        } else {
          this.isPromoApplied = false;
          this.resetDiscount();
          this.promoMessage = data?.message || 'Invalid or expired promo code.';
        }
      },
      error: (err) => {
        console.error('Promo validation error:', err);
        this.isPromoApplied = false;
        this.resetDiscount();
        this.promoMessage = err.error?.message || 'Failed to validate promo code. Please try again.';
      }
    });
  }

  selectPromo(code: string) {
    this.promoCode = code;
    this.applyPromo();
  }

  private resetDiscount() {
    this.discountRate = 0;
    this.flatDiscount = 0;
  }

  proceedToCheckout() {
    const codeToPass = this.promoCode.trim();
    this.router.navigate(['/checkout'], {
      queryParams: codeToPass ? { promoCode: codeToPass.toUpperCase() } : {}
    });
  }
  // --- Cart Operations ---
  updateQty(productId: number, currentQty: number, change: number, variantId?: number | null) {
    const newQty = currentQty + change;

    if (newQty < 1) {
      this.remove(productId, variantId);
    } else {
      this.cartService.updateQuantity(productId, newQty, variantId).subscribe({
        next: () => console.log(`Updated quantity for product ${productId}`),
        error: (err) => {
          console.error(err);
          this.toastService.error('Failed to update quantity. Check stock availability.');
        }
      });
    }
  }

  remove(cartItemId: number, variantId?: number | null) {
    this.loading.set(true);

    this.cartService.removeItem(cartItemId, variantId).subscribe({
      next: () => {
        this.cartItems.update(items => items.filter(item => item.id !== cartItemId));
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.cartItems.update(items => items.filter(item => item.id !== cartItemId));
        } else {
          this.toastService.error('Could not remove item. Server error.');
        }
      }
    });
  }

  async clearCart() {
    const confirmed = await this.confirmService.ask({
      title: 'Clear your cart?',
      message: 'This will remove all items from your cart. This can\'t be undone.',
      confirmLabel: 'Clear cart',
      danger: true
    });
    if (!confirmed) return;

    this.cartService.clearCart().subscribe({
      next: (res) => {
        if (res.success) {
          this.cartItems.set([]);
          this.toastService.success('Cart cleared.');
        } else {
          this.toastService.error(res.message || 'Failed to clear cart');
        }
      },
      error: (err) => {
        console.error('HTTP Error:', err);
        this.toastService.error('Failed to clear cart: Server error.');
      }
    });
  }
}