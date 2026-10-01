// import { Component, inject, signal, OnInit } from '@angular/core';
// import { CommonModule, CurrencyPipe } from '@angular/common';
// import { FormsModule } from '@angular/forms';
// import { Router, ActivatedRoute } from '@angular/router';
// import { CartService } from '../../Services/cart-service';
// import { OrderService } from '../../Services/order-service';
// import { OrderRequest, OrderResponse, PaymentRequest } from '../../Models/Order.model';
// import { ToastService } from '../../shared/toast/toast-service';
// import { AddressService } from '../../Services/address-service';
// import { AddressResponse, AddressRequest } from '../../Models/Address.model';

// // Razorpay's Checkout widget is loaded globally via a <script> tag in
// // index.html (see the CSP allowing checkout.razorpay.com), not as an npm
// // package - this declaration just tells TypeScript that global exists.
// declare const Razorpay: any;

// type CheckoutStep = 'shipping' | 'payment';

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
//   private router = inject(Router);
//   private toastService = inject(ToastService);
//   private addressService = inject(AddressService);
//   route = inject(ActivatedRoute);

//   // Get live cart data from your CartService signals
//   cart = this.cartService.cartData;
//   isProcessing = signal(false);

//   // Two-step checkout: fill shipping/method -> (card methods only) enter card details & pay
//   step = signal<CheckoutStep>('shipping');
//   // The order created (PENDING) while we wait for card payment to be verified
//   pendingOrder = signal<OrderResponse | null>(null);
//   paymentError = signal<string | null>(null);

//   // This matches your Java OrderRequest DTO perfectly
//   orderForm: OrderRequest = {
//     shippingAddress: '',
//     city: '',
//     state: '',
//     zipCode: '',
//     phoneNumber: '',
//     paymentMethod: 'CREDIT_CARD' // Default value
//   };

//   // Saved addresses - loaded on init, selectable to autofill the form above
//   // instead of retyping it every checkout.
//   savedAddresses = signal<AddressResponse[]>([]);
//   selectedAddressId = signal<number | null>(null);
//   showNewAddressForm = signal(false);
//   saveNewAddress = false;
//   newAddressLabel = '';

//   get isCardPayment(): boolean {
//     return this.orderForm.paymentMethod === 'CREDIT_CARD' || this.orderForm.paymentMethod === 'DEBIT_CARD';
//   }

//   ngOnInit() {
//     // Handle Buy Now - check for query params
//     const buyNowId = this.route.snapshot.queryParams['buyNow'];
//     const quantity = +this.route.snapshot.queryParams['quantity'] || 1;

//     if (buyNowId) {
//       console.log('Buy Now detected:', buyNowId, quantity);
//       this.handleBuyNow(+buyNowId, quantity);
//     }

//     this.loadSavedAddresses();
//   }

//   private loadSavedAddresses(): void {
//     this.addressService.getMyAddresses().subscribe({
//       next: (res) => {
//         const addresses = res.data || [];
//         this.savedAddresses.set(addresses);

//         if (addresses.length === 0) {
//           // No saved addresses yet - go straight to the manual form.
//           this.showNewAddressForm.set(true);
//           return;
//         }

//         // Auto-select the default (or the first one) so returning
//         // customers don't have to click anything to reuse their address.
//         const defaultAddress = addresses.find(a => a.isDefault) || addresses[0];
//         this.selectAddress(defaultAddress);
//       },
//       error: () => {
//         // Not fatal - checkout still works with the manual form, just
//         // without the convenience of saved addresses this time.
//         this.showNewAddressForm.set(true);
//       }
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

//   private handleBuyNow(productId: number, quantity: number) {
//     this.cartService.clearCart().subscribe({
//       next: () => {
//         this.cartService.addItemToCart(productId, quantity).subscribe({
//           next: () => console.log('Buy Now item added to cart'),
//           error: (err) => console.error('Failed to add Buy Now item:', err)
//         });
//       },
//       error: (err) => console.error('Failed to clear cart for Buy Now:', err)
//     });
//   }

//   /**
//    * Step 1: validate shipping info and place the order (created as PENDING
//    * on the backend). For COD we're done - the order is confirmed and paid
//    * on delivery. For card payments, we move to the in-page card entry step
//    * and only mark the order PAID once /api/payments/verify confirms it.
//    */
//   confirmOrder() {
//     if (!this.orderForm.shippingAddress || !this.orderForm.city) {
//       this.toastService.error('Please fill in all required fields.');
//       return;
//     }

//     this.isProcessing.set(true);
//     this.paymentError.set(null);

//     // If they're using a new address and checked "save this address",
//     // save it first (non-blocking - checkout proceeds either way even if
//     // the save fails, since that's a convenience feature, not critical path).
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
//         error: (err) => console.error('Could not save address for next time:', err),
//       });
//     }

//     this.placeOrder();
//   }

//   private placeOrder(): void {
//     this.orderService.checkout(this.orderForm).subscribe({
//       next: (res) => {
//         this.isProcessing.set(false);
//         const order: OrderResponse = res.data;

//         if (this.isCardPayment) {
//           // Hold onto the order and move to the card entry step - cart is
//           // cleared later, once payment is actually verified.
//           this.pendingOrder.set(order);
//           this.step.set('payment');
//         } else {
//           // Cash on Delivery: order is confirmed now, payment happens on delivery.
//           this.finishAndGoToSuccess(order);
//         }
//       },
//       error: (err) => {
//         this.isProcessing.set(false);
//         console.error('Order Error:', err);
//         this.toastService.error(err.error?.message || 'Failed to place order.');
//       }
//     });
//   }

//   /**
//    * Step 2 (Razorpay payments only): ask the backend to create a real
//    * Razorpay order for the amount it computed itself server-side, then
//    * open Razorpay's actual Checkout widget - this is where the customer
//    * enters card/UPI/netbanking details directly with Razorpay, never with
//    * us. On success, Razorpay hands back a signed payload that we send to
//    * our own /api/payments/verify endpoint, which cryptographically checks
//    * it actually came from Razorpay before marking the order PAID.
//    */
//   openRazorpayCheckout(): void {
//     const order = this.pendingOrder();
//     if (!order) return;

//     this.paymentError.set(null);
//     this.isProcessing.set(true);

//     this.orderService.createRazorpayOrder(order.orderId).subscribe({
//       next: (res) => {
//         this.isProcessing.set(false);
//         const rp = res.data;

//         const options = {
//           key: rp.keyId,
//           amount: rp.amountInPaise,
//           currency: rp.currency,
//           name: 'ShopSphere',
//           description: `Order #${order.orderId}`,
//           order_id: rp.razorpayOrderId,
//           prefill: {
//             contact: this.orderForm.phoneNumber,
//           },
//           theme: {
//             // Matches the app's coral brand color, not a default Razorpay blue.
//             color: '#FF3D71',
//           },
//           handler: (response: any) => {
//             // Called by Razorpay only after the customer actually
//             // completes payment - we still verify the signature
//             // server-side before trusting this.
//             this.verifyRazorpayPayment(order, response);
//           },
//           modal: {
//             ondismiss: () => {
//               // Customer closed the widget without paying - not an error,
//               // just let them retry from the same screen.
//               this.isProcessing.set(false);
//               this.paymentError.set('Payment was not completed. You can try again below.');
//             },
//           },
//         };

//         const razorpayCheckout = new Razorpay(options);
//         razorpayCheckout.on('payment.failed', (response: any) => {
//           this.isProcessing.set(false);
//           this.paymentError.set(
//             response?.error?.description || 'Payment failed. Please try again.'
//           );
//         });
//         razorpayCheckout.open();
//       },
//       error: (err) => {
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
//       status: 'SUCCESS', // informational only for Razorpay - the backend verifies the signature itself, it doesn't trust this
//       razorpayOrderId: razorpayResponse.razorpay_order_id,
//       razorpayPaymentId: razorpayResponse.razorpay_payment_id,
//       razorpaySignature: razorpayResponse.razorpay_signature,
//     };

//     this.orderService.verifyPayment(request).subscribe({
//       next: () => {
//         this.isProcessing.set(false);
//         const paidOrder: OrderResponse = { ...order, paymentStatus: 'PAID' };
//         this.finishAndGoToSuccess(paidOrder);
//       },
//       error: (err) => {
//         this.isProcessing.set(false);
//         console.error('Payment verification failed', err);
//         this.paymentError.set(
//           err.error?.message || 'We could not confirm your payment. If money was deducted, contact support with your order number.'
//         );
//       }
//     });
//   }

//   private finishAndGoToSuccess(order: OrderResponse) {
//     this.cartService.cartData.set(null);
//     this.router.navigate(['/order-success'], { state: { order } });
//   }

//   backToShipping(): void {
//     this.step.set('shipping');
//     this.paymentError.set(null);
//   }
// }

import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
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
 * customers being charged a price they never saw (e.g. MRP instead of the
 * discounted price). The real fix for such a mismatch belongs in the backend.
 */
const BLOCK_ON_PRICE_MISMATCH = true;
const PRICE_TOLERANCE = 1; // rupees

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe],
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
  route = inject(ActivatedRoute);

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
    if (buyNow) return buyNow.price * buyNow.quantity;
    return this.cart()?.totalAmount || 0;
  });

  discountAmount = computed(() => {
    const subtotal = this.cartSubtotal();
    const type = this.promoType();
    const value = this.promoValue();
    if (!this.appliedPromoCode() || !type || value == null) return 0;

    if (type === 'percent') {
      const rate = value > 1 ? value / 100 : value;
      return subtotal * rate;
    }
    return Math.min(value, subtotal);
  });

  cartFinalTotal = computed(() => {
    const final = this.cartSubtotal() - this.discountAmount();
    return final > 0 ? final : 0;
  });

  // Compares what the customer was shown with what the backend actually stored
  priceMismatch = computed(() => {
    const order = this.pendingOrder();
    if (!order || order.totalAmount == null) return false;
    return Math.abs(order.totalAmount - this.cartFinalTotal()) > PRICE_TOLERANCE;
  });

  blockPayment = computed(() => BLOCK_ON_PRICE_MISMATCH && this.priceMismatch());

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

  get isCardPayment(): boolean {
    return this.orderForm.paymentMethod === 'CREDIT_CARD' || this.orderForm.paymentMethod === 'DEBIT_CARD';
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

  private loadBuyNowProduct(productId: number, variantId: number | null): void {
    this.productService.getProductById(productId).subscribe({
      next: (res) => {
        if (!(res.success && res.data)) return;
        const product = res.data;
        const variant = variantId != null
          ? product.variants?.find(v => v.variantId === variantId)
          : undefined;

        const basePrice = variant?.priceOverride ?? product.price;
        const unitPrice = product.discountPercentage
          ? basePrice * (1 - product.discountPercentage / 100)
          : basePrice;

        this.buyNowSummaryItem.set({
          name: product.name,
          quantity: this.orderForm.quantity ?? 1,
          price: unitPrice,
          variantLabel: variant ? `${variant.size || ''} ${variant.color || ''}`.trim() : null
        });
      },
      error: () => this.toastService.error('Could not load the product for Buy Now.')
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
    this.orderForm.shippingAddress = address.shippingAddress;
    this.orderForm.city = address.city;
    this.orderForm.state = address.state;
    this.orderForm.zipCode = address.zipCode;
    this.orderForm.phoneNumber = address.phoneNumber;
  }

  useNewAddress(): void {
    this.selectedAddressId.set(null);
    this.showNewAddressForm.set(true);
    this.orderForm.shippingAddress = '';
    this.orderForm.city = '';
    this.orderForm.state = '';
    this.orderForm.zipCode = '';
    this.orderForm.phoneNumber = '';
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

    if (this.showNewAddressForm() && this.saveNewAddress && this.newAddressLabel.trim()) {
      const addressToSave: AddressRequest = {
        label: this.newAddressLabel.trim(),
        shippingAddress: this.orderForm.shippingAddress,
        city: this.orderForm.city,
        state: this.orderForm.state,
        zipCode: this.orderForm.zipCode,
        phoneNumber: this.orderForm.phoneNumber,
        isDefault: this.savedAddresses().length === 0,
      };
      this.addressService.createAddress(addressToSave).subscribe({
        error: (err: any) => console.error('Could not save address for next time:', err),
      });
    }

    // Details changed after an earlier order was created: cancel the stale one.
    const stale = this.pendingOrder();
    if (stale) {
      this.orderService.cancelOrder(stale.orderId).subscribe({
        error: (err: any) => console.error('Could not cancel stale order', err)
      });
      this.pendingOrder.set(null);
    }

    this.placeOrder();
  }

  private placeOrder(): void {
    this.orderService.checkout(this.orderForm).subscribe({
      next: (res) => {
        this.isProcessing.set(false);
        const order: OrderResponse = res.data;

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

  openRazorpayCheckout(): void {
    const order = this.pendingOrder();
    if (!order) return;

    if (this.blockPayment()) {
      this.paymentError.set(
        'The amount on this order does not match the price shown to you. Please go back and try again, or contact support.'
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
        this.finishAndGoToSuccess({ ...order, paymentStatus: 'PAID' });
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