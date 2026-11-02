
// import { Component, inject, signal, OnInit, computed } from '@angular/core';
// import { CommonModule, CurrencyPipe } from '@angular/common';
// import { FormsModule } from '@angular/forms';
// import { Router, ActivatedRoute } from '@angular/router';
// import { CartService } from '../../Services/cart-service';
// import { OrderService } from '../../Services/order-service';
// import { ProductService } from '../../Services/product-service';
// import { OrderRequest, OrderResponse, PaymentRequest } from '../../Models/Order.model';
// import { ToastService } from '../../shared/toast/toast-service';
// import { AddressService } from '../../Services/address-service';
// import { AddressResponse, AddressRequest } from '../../Models/Address.model';

// declare const Razorpay: any;

// type CheckoutStep = 'shipping' | 'payment';

// /**
//  * If the amount the backend stored on the order differs from what this page
//  * showed the customer, refuse to open Razorpay. This is a safety net: it stops
//  * customers being charged a price they never saw.
//  */
// const BLOCK_ON_PRICE_MISMATCH = true;
// const PRICE_TOLERANCE = 1; // rupees

// @Component({
//   selector: 'app-checkout',
//   standalone: true,
//   imports: [CommonModule, FormsModule, CurrencyPipe],
//   templateUrl: './checkout.html',
//   styleUrls: ['./checkout.css']
// })
// export class CheckoutPage implements OnInit {
//   private cartService = inject(CartService);
//   private orderService = inject(OrderService);
//   private productService = inject(ProductService);
//   private router = inject(Router);
//   private toastService = inject(ToastService);
//   private addressService = inject(AddressService);
//   route = inject(ActivatedRoute);

//   cart = this.cartService.cartData;
//   isProcessing = signal(false);

//   step = signal<CheckoutStep>('shipping');
//   pendingOrder = signal<OrderResponse | null>(null);
//   paymentError = signal<string | null>(null);
//   private lastOrderSnapshot: string | null = null;

//   // --- Promo state (only set once the backend confirms the code is usable) ---
//   appliedPromoCode = signal<string | null>(null);
//   promoType = signal<string | null>(null);
//   promoValue = signal<number | null | undefined>(null);
//   promoChecking = signal(false);

//   // Buy Now item (unit price already includes the product discount)
//   buyNowSummaryItem = signal<{
//     name: string;
//     quantity: number;
//     price: number;
//     variantLabel?: string | null;
//   } | null>(null);

//   // One subtotal for both flows
//   cartSubtotal = computed(() => {
//     const buyNow = this.buyNowSummaryItem();
//     if (buyNow) return buyNow.price * buyNow.quantity;
//     return this.cart()?.totalAmount || 0;
//   });

//   discountAmount = computed(() => {
//     const subtotal = this.cartSubtotal();
//     const type = this.promoType();
//     const value = this.promoValue();
//     if (!this.appliedPromoCode() || !type || value == null) return 0;

//     if (type === 'percent') {
//       const rate = value > 1 ? value / 100 : value;
//       return subtotal * rate;
//     }
//     return Math.min(value, subtotal);
//   });

//   cartFinalTotal = computed(() => {
//     const final = this.cartSubtotal() - this.discountAmount();
//     return final > 0 ? final : 0;
//   });

//   // Compares what the customer was shown with what the backend actually stored
//   priceMismatch = computed(() => {
//     const order = this.pendingOrder();
//     if (!order || order.totalAmount == null) return false;
//     return Math.abs(order.totalAmount - this.cartFinalTotal()) > PRICE_TOLERANCE;
//   });

//   blockPayment = computed(() => BLOCK_ON_PRICE_MISMATCH && this.priceMismatch());

//   orderForm: OrderRequest & {
//     promoCode?: string;
//     productId?: number;
//     quantity?: number;
//     variantId?: number;
//   } = {
//       shippingAddress: '',
//       city: '',
//       state: '',
//       zipCode: '',
//       phoneNumber: '',
//       paymentMethod: 'CREDIT_CARD',
//       promoCode: ''
//     };

//   savedAddresses = signal<AddressResponse[]>([]);
//   selectedAddressId = signal<number | null>(null);
//   showNewAddressForm = signal(false);
//   saveNewAddress = false;
//   newAddressLabel = '';

//   get isCardPayment(): boolean {
//     return this.orderForm.paymentMethod === 'CREDIT_CARD' || this.orderForm.paymentMethod === 'DEBIT_CARD';
//   }

//   ngOnInit() {
//     const params = this.route.snapshot.queryParams;
//     const buyNowId = params['buyNow'];
//     const quantity = params['quantity'];
//     const variantId = params['variantId'];
//     const promo = params['promoCode'];

//     if (buyNowId) {
//       this.orderForm.productId = +buyNowId;
//       this.orderForm.quantity = quantity ? +quantity : 1;
//       if (variantId) this.orderForm.variantId = +variantId;
//       this.loadBuyNowProduct(+buyNowId, variantId ? Number(variantId) : null);
//     } else {
//       // Refreshing /checkout used to leave the cart null; reload it.
//       this.cartService.loadCart().subscribe({
//         error: () => this.toastService.error('Could not load your cart.')
//       });
//     }

//     if (promo) this.validateAndApplyPromo(promo);

//     this.loadSavedAddresses();
//   }

//   private validateAndApplyPromo(code: string): void {
//     this.promoChecking.set(true);
//     this.cartService.validatePromoCode(code).subscribe({
//       next: (res) => {
//         this.promoChecking.set(false);
//         const data = res.data;
//         if (data?.valid && !data.used) {
//           this.appliedPromoCode.set(code);
//           this.orderForm.promoCode = code;
//           this.promoType.set(data.type ?? null);
//           this.promoValue.set(data.value);
//         } else {
//           this.clearPromo();
//           this.toastService.info(data?.message || `Promo code ${code} could not be applied.`);
//         }
//       },
//       error: () => {
//         this.promoChecking.set(false);
//         this.clearPromo();
//       }
//     });
//   }

//   private clearPromo(): void {
//     this.appliedPromoCode.set(null);
//     this.promoType.set(null);
//     this.promoValue.set(null);
//     this.orderForm.promoCode = '';
//   }

//   // onLoaded runs after the summary has been updated (or the request failed).
//   private loadBuyNowProduct(productId: number, variantId: number | null, onLoaded?: () => void): void {
//     this.productService.getProductById(productId).subscribe({
//       next: (res) => {
//         if (res.success && res.data) {
//           const product = res.data;
//           const variant = variantId != null
//             ? product.variants?.find(v => v.variantId === variantId)
//             : undefined;

//           const basePrice = variant?.priceOverride ?? product.price;
//           const unitPrice = product.discountPercentage
//             ? basePrice * (1 - product.discountPercentage / 100)
//             : basePrice;

//           this.buyNowSummaryItem.set({
//             name: product.name,
//             quantity: this.orderForm.quantity ?? 1,
//             price: unitPrice,
//             variantLabel: variant ? `${variant.size || ''} ${variant.color || ''}`.trim() : null
//           });
//         }
//         onLoaded?.();
//       },
//       error: () => {
//         this.toastService.error('Could not load the product for Buy Now.');
//         onLoaded?.();
//       }
//     });
//   }

//   private loadSavedAddresses(): void {
//     this.addressService.getMyAddresses().subscribe({
//       next: (res) => {
//         const addresses = res.data || [];
//         this.savedAddresses.set(addresses);

//         if (addresses.length === 0) {
//           this.showNewAddressForm.set(true);
//           return;
//         }
//         const defaultAddress = addresses.find(a => a.isDefault) || addresses[0];
//         this.selectAddress(defaultAddress);
//       },
//       error: () => this.showNewAddressForm.set(true)
//     });
//   }

//   selectAddress(address: AddressResponse): void {
//     this.selectedAddressId.set(address.addressId);
//     this.showNewAddressForm.set(false);
//     this.orderForm.shippingAddress = address.shippingAddress;
//     this.orderForm.city = address.city;
//     this.orderForm.state = address.state;
//     this.orderForm.zipCode = address.zipCode;
//     this.orderForm.phoneNumber = address.phoneNumber;
//   }

//   useNewAddress(): void {
//     this.selectedAddressId.set(null);
//     this.showNewAddressForm.set(true);
//     this.orderForm.shippingAddress = '';
//     this.orderForm.city = '';
//     this.orderForm.state = '';
//     this.orderForm.zipCode = '';
//     this.orderForm.phoneNumber = '';
//   }

//   private validateShipping(): string | null {
//     const f = this.orderForm;
//     if (!f.shippingAddress?.trim() || !f.city?.trim() || !f.state?.trim()) {
//       return 'Please fill in street address, city and state.';
//     }
//     if (!/^[1-9][0-9]{5}$/.test((f.zipCode || '').trim())) {
//       return 'Please enter a valid 6-digit PIN code.';
//     }
//     if (!/^(\+91[\s-]?)?[6-9][0-9]{9}$/.test((f.phoneNumber || '').replace(/\s|-/g, '').replace(/^\+91/, '+91'))) {
//       return 'Please enter a valid 10-digit Indian mobile number.';
//     }
//     return null;
//   }

//   private snapshot(): string {
//     const f = this.orderForm;
//     return JSON.stringify([
//       f.shippingAddress, f.city, f.state, f.zipCode, f.phoneNumber,
//       f.paymentMethod, f.promoCode, f.productId, f.quantity, f.variantId
//     ]);
//   }

//   confirmOrder() {
//     const error = this.validateShipping();
//     if (error) {
//       this.toastService.error(error);
//       return;
//     }

//     // Coming back from "Edit" with nothing changed: reuse the order we already
//     // created instead of creating a duplicate PENDING order.
//     if (this.isCardPayment && this.pendingOrder() && this.snapshot() === this.lastOrderSnapshot) {
//       this.paymentError.set(null);
//       this.step.set('payment');
//       return;
//     }

//     this.isProcessing.set(true);
//     this.paymentError.set(null);

//     // NEW: confirm the prices are still what the customer is looking at BEFORE any order is
//     // created. The page used the prices loaded earlier; if a price or discount changed in the
//     // meantime the server would charge the new price (correctly) but the customer had never seen
//     // it, and Cash on Delivery orders had no payment step to catch that.
//     this.verifyPricesThenContinue();
//   }

//   private verifyPricesThenContinue(): void {
//     const totalBefore = this.cartFinalTotal();

//     const afterRefresh = () => {
//       if (Math.abs(this.cartFinalTotal() - totalBefore) > 0.009) {
//         // The summary on the right already shows the new numbers.
//         this.isProcessing.set(false);
//         this.toastService.info(
//           'Prices were updated while you were checking out. Please review the new total, then continue.'
//         );
//         return;
//       }
//       this.createOrder();
//     };

//     if (this.orderForm.productId) {
//       this.loadBuyNowProduct(this.orderForm.productId, this.orderForm.variantId ?? null, afterRefresh);
//     } else {
//       this.cartService.loadCart().subscribe({
//         next: () => afterRefresh(),
//         error: () => {
//           this.isProcessing.set(false);
//           this.toastService.error('Could not verify your cart. Please try again.');
//         }
//       });
//     }
//   }

//   private createOrder(): void {
//     if (this.showNewAddressForm() && this.saveNewAddress && this.newAddressLabel.trim()) {
//       const addressToSave: AddressRequest = {
//         label: this.newAddressLabel.trim(),
//         shippingAddress: this.orderForm.shippingAddress,
//         city: this.orderForm.city,
//         state: this.orderForm.state,
//         zipCode: this.orderForm.zipCode,
//         phoneNumber: this.orderForm.phoneNumber,
//         isDefault: this.savedAddresses().length === 0,
//       };
//       this.addressService.createAddress(addressToSave).subscribe({
//         error: (err: any) => console.error('Could not save address for next time:', err),
//       });
//     }

//     // Details changed after an earlier order was created: cancel the stale one.
//     const stale = this.pendingOrder();
//     if (stale) {
//       this.orderService.cancelOrder(stale.orderId).subscribe({
//         error: (err: any) => console.error('Could not cancel stale order', err)
//       });
//       this.pendingOrder.set(null);
//     }

//     this.placeOrder();
//   }

//   private placeOrder(): void {
//     this.orderService.checkout(this.orderForm).subscribe({
//       next: (res) => {
//         this.isProcessing.set(false);
//         const order: OrderResponse = res.data;

//         if (this.isCardPayment) {
//           this.pendingOrder.set(order);
//           this.lastOrderSnapshot = this.snapshot();
//           this.step.set('payment');
//         } else {
//           this.finishAndGoToSuccess(order);
//         }
//       },
//       error: (err: any) => {
//         this.isProcessing.set(false);
//         console.error('Order Error:', err);
//         this.toastService.error(err.error?.message || 'Failed to place order.');
//       }
//     });
//   }

//   openRazorpayCheckout(): void {
//     const order = this.pendingOrder();
//     if (!order) return;

//     if (this.blockPayment()) {
//       this.paymentError.set(
//         'The amount on this order does not match the price shown to you. Please go back and try again, or contact support.'
//       );
//       return;
//     }

//     this.paymentError.set(null);
//     this.isProcessing.set(true);

//     this.orderService.createRazorpayOrder(order.orderId).subscribe({
//       next: (res) => {
//         this.isProcessing.set(false);
//         const rp = res?.data || res;

//         if (!rp || !rp.keyId) {
//           this.paymentError.set('Payment configuration error: Backend did not return keyId.');
//           return;
//         }

//         const options = {
//           key: rp.keyId,
//           amount: rp.amountInPaise,
//           currency: rp.currency || 'INR',
//           name: 'ShopSphere',
//           description: `Order #${order.orderId}`,
//           order_id: rp.razorpayOrderId,
//           prefill: { contact: this.orderForm.phoneNumber },
//           theme: { color: '#FF3D71' },
//           handler: (response: any) => this.verifyRazorpayPayment(order, response),
//           modal: {
//             ondismiss: () => {
//               this.isProcessing.set(false);
//               this.paymentError.set('Payment was not completed. You can try again below.');
//             },
//           },
//         };

//         const razorpayCheckout = new Razorpay(options);
//         razorpayCheckout.on('payment.failed', (response: any) => {
//           this.isProcessing.set(false);
//           this.paymentError.set(response?.error?.description || 'Payment failed. Please try again.');
//         });
//         razorpayCheckout.open();
//       },
//       error: (err: any) => {
//         this.isProcessing.set(false);
//         this.paymentError.set(err.error?.message || 'Could not start payment. Please try again.');
//       }
//     });
//   }

//   private verifyRazorpayPayment(order: OrderResponse, razorpayResponse: any): void {
//     this.isProcessing.set(true);

//     const request: PaymentRequest = {
//       orderId: order.orderId,
//       transactionId: razorpayResponse.razorpay_payment_id,
//       paymentMethod: this.orderForm.paymentMethod,
//       paymentGateway: 'RAZORPAY',
//       status: 'SUCCESS',
//       razorpayOrderId: razorpayResponse.razorpay_order_id,
//       razorpayPaymentId: razorpayResponse.razorpay_payment_id,
//       razorpaySignature: razorpayResponse.razorpay_signature,
//     };

//     this.orderService.verifyPayment(request).subscribe({
//       next: () => {
//         this.isProcessing.set(false);
//         // A card order comes back from the server as PENDING until payment is verified, so the
//         // success page must be told it is now CONFIRMED as well as PAID. Before, it showed
//         // "Order Confirmed!" next to a status pill saying PENDING.
//         this.finishAndGoToSuccess({ ...order, paymentStatus: 'PAID', orderStatus: 'CONFIRMED' });
//       },
//       error: (err: any) => {
//         this.isProcessing.set(false);
//         this.paymentError.set(
//           err.error?.message || 'We could not confirm your payment. If money was deducted, contact support with your order number.'
//         );
//       }
//     });
//   }

//   private finishAndGoToSuccess(order: OrderResponse) {
//     if (!this.orderForm.productId) {
//       this.cartService.cartData.set(null);
//     }
//     this.router.navigate(['/order-success'], { state: { order } });
//   }

//   backToShipping(): void {
//     this.step.set('shipping');
//     this.paymentError.set(null);
//   }
// }

import { Component, inject, signal, OnInit, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { CartService } from '../../Services/cart-service';
import { OrderService } from '../../Services/order-service';
import { ProductService } from '../../Services/product-service';
import { OrderRequest, OrderResponse, PaymentRequest } from '../../Models/Order.model';
import { ToastService } from '../../shared/toast/toast-service';
import { AddressService } from '../../Services/address-service';
import { AddressResponse, AddressRequest } from '../../Models/Address.model';

declare const Razorpay: any;

type CheckoutStep = 'shipping' | 'payment';

/**
 * If the amount the backend stored on the order differs from what this page
 * showed the customer, refuse to open Razorpay. This is a safety net: it stops
 * customers being charged a price they never saw.
 */
const BLOCK_ON_PRICE_MISMATCH = true;
const PRICE_TOLERANCE = 1; // rupees

// NEW: a fixed list, so "tamilnadu" / "Tamil Nadu" / "TN" can no longer end up as three different
// states in the database, in emails and in the admin panel.
const INDIAN_STATES = [
  'Andaman and Nicobar Islands', 'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chandigarh',
  'Chhattisgarh', 'Dadra and Nagar Haveli and Daman and Diu', 'Delhi', 'Goa', 'Gujarat', 'Haryana',
  'Himachal Pradesh', 'Jammu and Kashmir', 'Jharkhand', 'Karnataka', 'Kerala', 'Ladakh', 'Lakshadweep',
  'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Puducherry',
  'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand',
  'West Bengal',
];

// "Tamil Nadu", "tamilnadu" and "Tamil  Nadu " all become "tamilnadu"; "&" becomes "and".
const norm = (s: string | null | undefined): string =>
  (s ?? '').toLowerCase().replace(/&/g, 'and').replace(/[^a-z]/g, '');

const round2 = (n: number): number => Math.round(n * 100) / 100;

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, RouterLink],
  templateUrl: './checkout.html',
  styleUrls: ['./checkout.css']
})
export class CheckoutPage implements OnInit {
  private cartService = inject(CartService);
  private orderService = inject(OrderService);
  private productService = inject(ProductService);
  private router = inject(Router);
  private toastService = inject(ToastService);
  private addressService = inject(AddressService);
  private cdr = inject(ChangeDetectorRef);
  route = inject(ActivatedRoute);

  readonly states = INDIAN_STATES;

  cart = this.cartService.cartData;
  isProcessing = signal(false);

  step = signal<CheckoutStep>('shipping');
  pendingOrder = signal<OrderResponse | null>(null);
  paymentError = signal<string | null>(null);
  private lastOrderSnapshot: string | null = null;

  // --- Promo state (only set once the backend confirms the code is usable) ---
  appliedPromoCode = signal<string | null>(null);
  promoType = signal<string | null>(null);
  promoValue = signal<number | null | undefined>(null);
  promoChecking = signal(false);

  // Buy Now item (unit price already includes the product discount)
  buyNowSummaryItem = signal<{
    name: string;
    quantity: number;
    price: number;
    variantLabel?: string | null;
  } | null>(null);

  // One subtotal for both flows
  cartSubtotal = computed(() => {
    const buyNow = this.buyNowSummaryItem();
    if (buyNow) return round2(buyNow.price * buyNow.quantity);
    return this.cart()?.totalAmount || 0;
  });

  discountAmount = computed(() => {
    const subtotal = this.cartSubtotal();
    const type = this.promoType();
    const value = this.promoValue();
    if (!this.appliedPromoCode() || !type || value == null) return 0;

    if (type === 'percent') {
      // CHANGED: ">= 1" (was "> 1") so it matches the server: 1 means 1%, 0.10 and 10 both mean 10%.
      const rate = value >= 1 ? value / 100 : value;
      return round2(Math.min(subtotal * rate, subtotal));
    }
    return Math.min(value, subtotal);
  });

  cartFinalTotal = computed(() => {
    const final = this.cartSubtotal() - this.discountAmount();
    return final > 0 ? round2(final) : 0;
  });

  // Compares what the customer was shown with what the backend actually stored
  priceMismatch = computed(() => {
    const order = this.pendingOrder();
    if (!order || order.totalAmount == null) return false;
    return Math.abs(order.totalAmount - this.cartFinalTotal()) > PRICE_TOLERANCE;
  });

  blockPayment = computed(() => BLOCK_ON_PRICE_MISMATCH && this.priceMismatch());

  // NEW: the button used to stay clickable with an empty bag and only fail at the server.
  canSubmit = computed(() => {
    if (this.buyNowSummaryItem()) return true;
    return (this.cart()?.items?.length ?? 0) > 0;
  });

  orderForm: OrderRequest & {
    promoCode?: string;
    productId?: number;
    quantity?: number;
    variantId?: number;
  } = {
      shippingAddress: '',
      city: '',
      state: '',
      zipCode: '',
      phoneNumber: '',
      paymentMethod: 'CREDIT_CARD',
      promoCode: ''
    };

  savedAddresses = signal<AddressResponse[]>([]);
  selectedAddressId = signal<number | null>(null);
  showNewAddressForm = signal(false);
  saveNewAddress = false;
  newAddressLabel = '';

  // NEW: a gentle, never-blocking hint when the PIN code does not match the city/state typed.
  pinHint = signal<string | null>(null);
  private lastLookedUpPin = '';

  get isCardPayment(): boolean {
    return this.orderForm.paymentMethod === 'CREDIT_CARD' || this.orderForm.paymentMethod === 'DEBIT_CARD';
  }

  // NEW: "Proceed to Checkout" made no sense on the checkout page itself.
  get submitLabel(): string {
    return this.isCardPayment ? 'Continue to Payment' : 'Place Order';
  }

  // True once the cart has loaded and is really empty (not while it is still loading).
  get cartIsEmpty(): boolean {
    return !this.orderForm.productId && this.cart() != null && (this.cart()?.items?.length ?? 0) === 0;
  }

  ngOnInit() {
    const params = this.route.snapshot.queryParams;
    const buyNowId = params['buyNow'];
    const quantity = params['quantity'];
    const variantId = params['variantId'];
    const promo = params['promoCode'];

    if (buyNowId) {
      this.orderForm.productId = +buyNowId;
      this.orderForm.quantity = quantity ? +quantity : 1;
      if (variantId) this.orderForm.variantId = +variantId;
      this.loadBuyNowProduct(+buyNowId, variantId ? Number(variantId) : null);
    } else {
      // Refreshing /checkout used to leave the cart null; reload it.
      this.cartService.loadCart().subscribe({
        error: () => this.toastService.error('Could not load your cart.')
      });
    }

    if (promo) this.validateAndApplyPromo(promo);

    this.loadSavedAddresses();
  }

  private validateAndApplyPromo(code: string): void {
    this.promoChecking.set(true);
    this.cartService.validatePromoCode(code).subscribe({
      next: (res) => {
        this.promoChecking.set(false);
        const data = res.data;
        if (data?.valid && !data.used) {
          this.appliedPromoCode.set(code);
          this.orderForm.promoCode = code;
          this.promoType.set(data.type ?? null);
          this.promoValue.set(data.value);
        } else {
          this.clearPromo();
          this.toastService.info(data?.message || `Promo code ${code} could not be applied.`);
        }
      },
      error: () => {
        this.promoChecking.set(false);
        this.clearPromo();
      }
    });
  }

  private clearPromo(): void {
    this.appliedPromoCode.set(null);
    this.promoType.set(null);
    this.promoValue.set(null);
    this.orderForm.promoCode = '';
  }

  // onLoaded runs after the summary has been updated (or the request failed).
  private loadBuyNowProduct(productId: number, variantId: number | null, onLoaded?: () => void): void {
    this.productService.getProductById(productId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          const product = res.data;
          const variant = variantId != null
            ? product.variants?.find(v => v.variantId === variantId)
            : undefined;

          const basePrice = variant?.priceOverride ?? product.price;
          // CHANGED: rounded to paise, like the server does, so the totals agree to the paisa.
          const unitPrice = product.discountPercentage
            ? round2(basePrice * (1 - product.discountPercentage / 100))
            : basePrice;

          this.buyNowSummaryItem.set({
            name: product.name,
            quantity: this.orderForm.quantity ?? 1,
            price: unitPrice,
            variantLabel: variant ? `${variant.size || ''} ${variant.color || ''}`.trim() : null
          });
        }
        onLoaded?.();
      },
      error: () => {
        this.toastService.error('Could not load the product for Buy Now.');
        onLoaded?.();
      }
    });
  }

  private loadSavedAddresses(): void {
    this.addressService.getMyAddresses().subscribe({
      next: (res) => {
        const addresses = res.data || [];
        this.savedAddresses.set(addresses);

        if (addresses.length === 0) {
          this.showNewAddressForm.set(true);
          return;
        }
        const defaultAddress = addresses.find(a => a.isDefault) || addresses[0];
        this.selectAddress(defaultAddress);
      },
      error: () => this.showNewAddressForm.set(true)
    });
  }

  selectAddress(address: AddressResponse): void {
    this.selectedAddressId.set(address.addressId);
    this.showNewAddressForm.set(false);
    this.pinHint.set(null);
    this.orderForm.shippingAddress = address.shippingAddress;
    this.orderForm.city = address.city;
    this.orderForm.state = address.state;
    this.orderForm.zipCode = address.zipCode;
    this.orderForm.phoneNumber = address.phoneNumber;
  }

  useNewAddress(): void {
    this.selectedAddressId.set(null);
    this.showNewAddressForm.set(true);
    this.pinHint.set(null);
    this.lastLookedUpPin = '';
    this.orderForm.shippingAddress = '';
    this.orderForm.city = '';
    this.orderForm.state = '';
    this.orderForm.zipCode = '';
    this.orderForm.phoneNumber = '';
  }

  // --- NEW: PIN code helper ---------------------------------------------------------------
  // Looks the PIN up on India Post's free public directory and fills in an EMPTY city/state.
  // It is best-effort: if the service is slow or down, nothing happens and checkout works as before.
  // It uses fetch() on purpose, NOT HttpClient: an HttpClient interceptor that adds your login
  // token to requests would otherwise send that token to a third-party website.
  onPinChange(value: string): void {
    const pin = (value || '').replace(/\D/g, '').slice(0, 6);
    this.orderForm.zipCode = pin;
    this.pinHint.set(null);

    if (pin.length < 6) {
      this.lastLookedUpPin = '';
      return;
    }
    if (pin !== this.lastLookedUpPin) {
      this.lastLookedUpPin = pin;
      void this.lookupPin(pin);
    }
  }

  private async lookupPin(pin: string): Promise<void> {
    try {
      const response = await fetch(`https://api.postalpincode.in/pincode/${pin}`);
      if (!response.ok) return;
      const body = await response.json();
      const result = Array.isArray(body) ? body[0] : null;

      if (result?.Status !== 'Success' || !result.PostOffice?.length) {
        if (result?.Status === 'Error') {
          this.pinHint.set('We could not find this PIN code. Please double-check it.');
        }
        return;
      }

      // The user may have typed a different PIN while we waited.
      if (this.orderForm.zipCode !== pin) return;

      const office = result.PostOffice[0];
      const district: string = (office.District ?? '').trim();
      const stateName = INDIAN_STATES.find(s => norm(s) === norm(office.State));

      if (!this.orderForm.city.trim() && district) this.orderForm.city = district;
      if (!this.orderForm.state.trim() && stateName) this.orderForm.state = stateName;

      const city = norm(this.orderForm.city);
      const cityOk = !city || !district || city.includes(norm(district)) || norm(district).includes(city);
      const stateOk = !stateName || norm(this.orderForm.state) === norm(stateName);

      if (!cityOk || !stateOk) {
        this.pinHint.set(
          `PIN ${pin} belongs to ${district}, ${stateName ?? office.State}. Please check your city and state.`
        );
      }
    } catch {
      // Offline or the service is down: silently carry on.
    } finally {
      this.cdr.markForCheck();
    }
  }

  private validateShipping(): string | null {
    const f = this.orderForm;
    if (!f.shippingAddress?.trim() || !f.city?.trim() || !f.state?.trim()) {
      return 'Please fill in street address, city and state.';
    }
    if (!/^[1-9][0-9]{5}$/.test((f.zipCode || '').trim())) {
      return 'Please enter a valid 6-digit PIN code.';
    }
    if (!/^(\+91[\s-]?)?[6-9][0-9]{9}$/.test((f.phoneNumber || '').replace(/\s|-/g, '').replace(/^\+91/, '+91'))) {
      return 'Please enter a valid 10-digit Indian mobile number.';
    }
    return null;
  }

  private snapshot(): string {
    const f = this.orderForm;
    return JSON.stringify([
      f.shippingAddress, f.city, f.state, f.zipCode, f.phoneNumber,
      f.paymentMethod, f.promoCode, f.productId, f.quantity, f.variantId
    ]);
  }

  confirmOrder() {
    if (!this.canSubmit()) {
      this.toastService.error('Your bag is empty. Add something to continue.');
      return;
    }

    const error = this.validateShipping();
    if (error) {
      this.toastService.error(error);
      return;
    }

    // Coming back from "Edit" with nothing changed: reuse the order we already
    // created instead of creating a duplicate PENDING order.
    if (this.isCardPayment && this.pendingOrder() && this.snapshot() === this.lastOrderSnapshot) {
      this.paymentError.set(null);
      this.step.set('payment');
      return;
    }

    this.isProcessing.set(true);
    this.paymentError.set(null);

    // Confirm the prices are still what the customer is looking at BEFORE any order is created.
    this.verifyPricesThenContinue();
  }

  private verifyPricesThenContinue(): void {
    const totalBefore = this.cartFinalTotal();

    const afterRefresh = () => {
      if (Math.abs(this.cartFinalTotal() - totalBefore) > 0.009) {
        // The summary on the right already shows the new numbers.
        this.isProcessing.set(false);
        this.toastService.info(
          'Prices were updated while you were checking out. Please review the new total, then continue.'
        );
        return;
      }
      this.createOrder();
    };

    if (this.orderForm.productId) {
      this.loadBuyNowProduct(this.orderForm.productId, this.orderForm.variantId ?? null, afterRefresh);
    } else {
      this.cartService.loadCart().subscribe({
        next: () => afterRefresh(),
        error: () => {
          this.isProcessing.set(false);
          this.toastService.error('Could not verify your cart. Please try again.');
        }
      });
    }
  }

  private createOrder(): void {
    // Details changed after an earlier order was created: cancel the stale one FIRST and only
    // then place the new one. They used to run side by side, so the new order could be refused
    // ("promo code already used", or "out of stock" for the last unit) because the old order had
    // not finished releasing its promo code and stock yet.
    const stale = this.pendingOrder();
    if (stale) {
      this.pendingOrder.set(null);
      this.orderService.cancelOrder(stale.orderId).subscribe({
        next: () => this.placeOrder(),
        error: (err: any) => {
          console.error('Could not cancel stale order', err);
          this.placeOrder();
        }
      });
      return;
    }
    this.placeOrder();
  }

  private placeOrder(): void {
    this.orderService.checkout(this.orderForm).subscribe({
      next: (res) => {
        this.isProcessing.set(false);
        const order: OrderResponse = res.data;

        // CHANGED: the address is saved only AFTER the order exists. It used to be saved first,
        // even when the order then failed.
        this.saveAddressIfRequested();

        if (this.isCardPayment) {
          this.pendingOrder.set(order);
          this.lastOrderSnapshot = this.snapshot();
          this.step.set('payment');
        } else {
          this.finishAndGoToSuccess(order);
        }
      },
      error: (err: any) => {
        this.isProcessing.set(false);
        console.error('Order Error:', err);
        this.toastService.error(err.error?.message || 'Failed to place order.');
      }
    });
  }

  private saveAddressIfRequested(): void {
    if (!(this.showNewAddressForm() && this.saveNewAddress)) return;

    // CHANGED: an empty label used to make the "Save this address" box do NOTHING, silently.
    const label = this.newAddressLabel.trim() || 'Home';

    const addressToSave: AddressRequest = {
      label,
      shippingAddress: this.orderForm.shippingAddress,
      city: this.orderForm.city,
      state: this.orderForm.state,
      zipCode: this.orderForm.zipCode,
      phoneNumber: this.orderForm.phoneNumber,
      isDefault: this.savedAddresses().length === 0,
    };

    // Stop re-saving if the customer goes back and forth between the two steps.
    this.saveNewAddress = false;

    this.addressService.createAddress(addressToSave).subscribe({
      next: (res) => {
        const saved = res.data;
        if (saved) {
          this.savedAddresses.update(list => [...list, saved]);
          this.selectedAddressId.set(saved.addressId);
          this.showNewAddressForm.set(false);
        }
      },
      error: (err: any) => {
        const message: string | undefined = err.error?.message;
        // "You already saved this address." is fine, nothing to tell the customer.
        if (message && !/already saved/i.test(message)) {
          this.toastService.info(`Your order was placed, but the address could not be saved: ${message}`);
        }
      },
    });
  }

  openRazorpayCheckout(): void {
    const order = this.pendingOrder();
    if (!order) return;

    if (this.blockPayment()) {
      this.paymentError.set(
        'The amount on this order does not match the price shown to you. Please go back and try again, or contact support.'
      );
      return;
    }

    // NEW: if the Razorpay script is blocked (ad blocker, offline), say so instead of a raw error.
    if (typeof Razorpay === 'undefined') {
      this.paymentError.set(
        'The payment window could not load. Please check your internet connection, turn off any ad blocker, and refresh the page.'
      );
      return;
    }

    this.paymentError.set(null);
    this.isProcessing.set(true);

    this.orderService.createRazorpayOrder(order.orderId).subscribe({
      next: (res) => {
        this.isProcessing.set(false);
        const rp = res?.data || res;

        if (!rp || !rp.keyId) {
          this.paymentError.set('Payment configuration error: Backend did not return keyId.');
          return;
        }

        const options = {
          key: rp.keyId,
          amount: rp.amountInPaise,
          currency: rp.currency || 'INR',
          name: 'ShopSphere',
          description: `Order #${order.orderId}`,
          order_id: rp.razorpayOrderId,
          prefill: { contact: this.orderForm.phoneNumber },
          theme: { color: '#FF3D71' },
          handler: (response: any) => this.verifyRazorpayPayment(order, response),
          modal: {
            ondismiss: () => {
              this.isProcessing.set(false);
              this.paymentError.set('Payment was not completed. You can try again below.');
            },
          },
        };

        const razorpayCheckout = new Razorpay(options);
        razorpayCheckout.on('payment.failed', (response: any) => {
          this.isProcessing.set(false);
          this.paymentError.set(response?.error?.description || 'Payment failed. Please try again.');
        });
        razorpayCheckout.open();
      },
      error: (err: any) => {
        this.isProcessing.set(false);
        this.paymentError.set(err.error?.message || 'Could not start payment. Please try again.');
      }
    });
  }

  private verifyRazorpayPayment(order: OrderResponse, razorpayResponse: any): void {
    this.isProcessing.set(true);

    const request: PaymentRequest = {
      orderId: order.orderId,
      transactionId: razorpayResponse.razorpay_payment_id,
      paymentMethod: this.orderForm.paymentMethod,
      paymentGateway: 'RAZORPAY',
      status: 'SUCCESS',
      razorpayOrderId: razorpayResponse.razorpay_order_id,
      razorpayPaymentId: razorpayResponse.razorpay_payment_id,
      razorpaySignature: razorpayResponse.razorpay_signature,
    };

    this.orderService.verifyPayment(request).subscribe({
      next: () => {
        this.isProcessing.set(false);
        // A card order comes back from the server as PENDING until payment is verified, so the
        // success page must be told it is now CONFIRMED as well as PAID.
        this.finishAndGoToSuccess({ ...order, paymentStatus: 'PAID', orderStatus: 'CONFIRMED' });
      },
      error: (err: any) => {
        this.isProcessing.set(false);
        this.paymentError.set(
          err.error?.message || 'We could not confirm your payment. If money was deducted, contact support with your order number.'
        );
      }
    });
  }

  private finishAndGoToSuccess(order: OrderResponse) {
    if (!this.orderForm.productId) {
      this.cartService.cartData.set(null);
    }
    this.router.navigate(['/order-success'], { state: { order } });
  }

  backToShipping(): void {
    this.step.set('shipping');
    this.paymentError.set(null);
  }
}