
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
  loading = signal(false);
  initialLoading = signal(true);
  loadError = signal(false);

  // --- Promo Code State ---
  promoCode: string = '';
  promoMessage: string = '';
  isPromoApplied: boolean = false;
  discountRate: number = 0;
  flatDiscount: number = 0;
  promoMinOrder: number | null = null;
  // The code that was actually validated. proceedToCheckout used to pass whatever was currently
  // typed in the box, even if it had never been applied (or was edited after applying).
  private appliedCode: string | null = null;

  ngOnInit(): void {
    this.loadCartData();
  }

  loadCartData() {
    this.loadError.set(false);
    this.initialLoading.set(true);
    this.cartService.loadCart().subscribe({
      next: () => this.initialLoading.set(false),
      error: (err) => {
        console.error('Failed to load cart on init:', err);
        this.initialLoading.set(false);
        // Used to show "Your bag is empty" when the server simply failed.
        this.loadError.set(true);
        this.toastService.error('Could not load your cart. Please try again.');
      }
    });
  }

  // --- Calculated Order Totals ---
  get subtotal(): number {
    return this.cart()?.totalAmount || 0;
  }

  // A code with a minimum order value stops counting if the cart drops below it.
  get promoLocked(): boolean {
    return this.isPromoApplied && this.promoMinOrder != null && this.subtotal < this.promoMinOrder;
  }

  get discountAmount(): number {
    if (!this.isPromoApplied || this.promoLocked) return 0;
    const raw = this.discountRate > 0 ? this.subtotal * this.discountRate : this.flatDiscount;
    return Math.min(raw, this.subtotal);   // a flat discount can never exceed the cart
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
      this.clearPromo();
      return;
    }

    this.cartService.validatePromoCode(code).subscribe({
      next: (res) => {
        const data = res.data;

        if (data?.used) {
          this.promoMessage = data.message || 'You have already used this promo code on a previous order.';
          this.clearPromo();
        } else if (data?.valid && data.value != null) {
          this.isPromoApplied = true;
          this.appliedCode = code;
          this.promoMinOrder = data.minOrderAmount ?? null;

          if (data.type === 'percent') {
            // 0.10 and 10 both mean 10%: the same rule the server applies. Before, a coupon stored
            // as 10 made the discount 1000% of the cart.
            this.discountRate = data.value > 1 ? data.value / 100 : data.value;
            this.flatDiscount = 0;
            this.promoMessage = `Code applied! ${Math.round(this.discountRate * 100)}% discount added.`;
          } else {
            this.flatDiscount = data.value;
            this.discountRate = 0;
            this.promoMessage = `Code applied! ₹${data.value} discount added.`;
          }
        } else {
          this.clearPromo();
          this.promoMessage = data?.message || 'Invalid or expired promo code.';
        }
      },
      error: (err) => {
        console.error('Promo validation error:', err);
        this.clearPromo();
        this.promoMessage = err.error?.message || 'Failed to validate promo code. Please try again.';
      }
    });
  }

  selectPromo(code: string) {
    this.promoCode = code;
    this.applyPromo();
  }

  private clearPromo() {
    this.isPromoApplied = false;
    this.appliedCode = null;
    this.promoMinOrder = null;
    this.discountRate = 0;
    this.flatDiscount = 0;
  }

  proceedToCheckout() {
    const codeToPass = this.isPromoApplied && !this.promoLocked ? this.appliedCode : null;
    this.router.navigate(['/checkout'], {
      queryParams: codeToPass ? { promoCode: codeToPass } : {}
    });
  }

  // --- Cart Operations ---
  updateQty(productId: number, currentQty: number, change: number, variantId?: number | null) {
    const newQty = currentQty + change;

    if (newQty < 1) {
      this.remove(productId, variantId);
      return;
    }

    this.loading.set(true);
    this.cartService.updateQuantity(productId, newQty, variantId).subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        // The server says exactly what is wrong (e.g. not enough stock).
        this.toastService.error(err.error?.message || 'Failed to update quantity.');
      }
    });
  }

  // (The first argument was called "cartItemId", but it is the PRODUCT id.)
  remove(productId: number, variantId?: number | null) {
    this.loading.set(true);

    this.cartService.removeItem(productId, variantId).subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        if (err.status === 404) {
          // Already gone on the server (another tab?). Show the real cart.
          this.cartService.loadCart().subscribe({ error: () => { } });
        } else {
          this.toastService.error(err.error?.message || 'Could not remove item.');
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
          this.clearPromo();
          this.toastService.success('Cart cleared.');
        } else {
          this.toastService.error(res.message || 'Failed to clear cart');
        }
      },
      error: (err) => {
        this.toastService.error(err.error?.message || 'Failed to clear cart.');
      }
    });
  }
  // Removed: the unused cartItems signal, and the console.log calls.
}