// import { Component, OnInit, inject, signal, computed } from '@angular/core';
// import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
// import { FormsModule } from '@angular/forms';
// import { ActivatedRoute, RouterLink } from '@angular/router';
// import { Title, Meta } from '@angular/platform-browser';
// import { ProductService } from '../../Services/product-service';
// import { CartService } from '../../Services/cart-service';
// import { WishlistService } from '../../Services/wishlist-service';
// import { AuthService } from '../../Services/auth-service';
// import { ReviewService } from '../../Services/review-service';
// import { ToastService } from '../../shared/toast/toast-service';
// import { ProductResponse, ProductVariantResponse } from '../../Models/Product.model';
// import { ReviewResponse, ReviewEligibilityResponse } from '../../Models/Review.model';
// import { productImageUrl } from '../../shared/api.config';

// @Component({
//   selector: 'app-product-detail',
//   standalone: true,
//   imports: [CommonModule, CurrencyPipe, DatePipe, FormsModule, RouterLink],
//   templateUrl: './product-detail.html',
//   styleUrls: ['./product-detail.css']
// })
// export class ProductDetail implements OnInit {
//   private route = inject(ActivatedRoute);
//   private productService = inject(ProductService);
//   private cartService = inject(CartService);
//   private wishlistService = inject(WishlistService);
//   private authService = inject(AuthService);
//   private reviewService = inject(ReviewService);
//   private toastService = inject(ToastService);
//   private titleService = inject(Title);
//   private metaService = inject(Meta);

//   protected readonly productImageUrl = productImageUrl;

//   product = signal<ProductResponse | null>(null);
//   loading = signal(true);
//   notFound = signal(false);
//   quantity = signal(1);

//   // --- Variants ---
//   // Selection is tracked as size/color separately so a product that only
//   // varies by one axis (e.g. a single-size accessory in different colors)
//   // doesn't force the customer through a redundant "size" choice.
//   selectedSize = signal<string | null>(null);
//   selectedColor = signal<string | null>(null);

//   hasVariants = computed(() => (this.product()?.variants?.length ?? 0) > 0);

//   availableSizes = computed(() => {
//     const sizes = (this.product()?.variants ?? [])
//       .map(v => v.size)
//       .filter((s): s is string => !!s);
//     return Array.from(new Set(sizes));
//   });

//   availableColors = computed(() => {
//     const colors = (this.product()?.variants ?? [])
//       .map(v => v.color)
//       .filter((c): c is string => !!c);
//     return Array.from(new Set(colors));
//   });

//   selectedVariant = computed<ProductVariantResponse | null>(() => {
//     const variants = this.product()?.variants ?? [];
//     if (variants.length === 0) return null;

//     const needsSize = this.availableSizes().length > 0;
//     const needsColor = this.availableColors().length > 0;
//     if (needsSize && !this.selectedSize()) return null;
//     if (needsColor && !this.selectedColor()) return null;

//     return variants.find(v =>
//       (!needsSize || v.size === this.selectedSize()) &&
//       (!needsColor || v.color === this.selectedColor())
//     ) ?? null;
//   });

//   // Effective stock/price for whatever's currently selected - falls back
//   // to the base product for simple (non-variant) products.
//   effectiveStock = computed(() => {
//     const product = this.product();
//     if (!product) return 0;
//     if (!this.hasVariants()) return product.stock;
//     return this.selectedVariant()?.stock ?? 0;
//   });

//   effectivePrice = computed(() => {
//     const product = this.product();
//     if (!product) return 0;
//     const override = this.hasVariants() ? this.selectedVariant()?.priceOverride : null;
//     const base = override ?? product.price;
//     if (!product.discountPercentage) return base;
//     return base * (1 - product.discountPercentage / 100);
//   });

//   // --- Reviews ---
//   reviews = signal<ReviewResponse[]>([]);
//   reviewsLoading = signal(true);
//   eligibility = signal<ReviewEligibilityResponse | null>(null);
//   showReviewForm = signal(false);
//   submittingReview = signal(false);
//   newReviewRating = signal(0);
//   newReviewComment = '';

//   ngOnInit(): void {
//     const idParam = this.route.snapshot.paramMap.get('id');
//     const productId = idParam ? Number(idParam) : NaN;

//     if (!idParam || isNaN(productId)) {
//       this.notFound.set(true);
//       this.loading.set(false);
//       return;
//     }

//     this.productService.getProductById(productId).subscribe({
//       next: (res) => {
//         this.loading.set(false);
//         if (res.success && res.data) {
//           this.product.set(res.data);
//           this.setSeoTags(res.data);
//           this.preselectSingleOptionVariants(res.data);
//         } else {
//           this.notFound.set(true);
//         }
//       },
//       error: (err) => {
//         this.loading.set(false);
//         // BUG FIX: this used to set notFound for ANY error (network issue,
//         // 500, etc.), showing the misleading "Product not found" message
//         // even when the product exists and something else genuinely went
//         // wrong. Only a real 404 means "doesn't exist" - anything else
//         // gets a distinct, honest error state instead.
//         if (err.status === 404) {
//           this.notFound.set(true);
//         } else {
//           this.loadError.set(true);
//         }
//       }
//     });

//     this.loadReviews(productId);

//     if (this.authService.isLoggedIn()) {
//       this.reviewService.checkEligibility(productId).subscribe({
//         next: (res) => this.eligibility.set(res.data ?? null),
//         error: (err) => console.error('Failed to check review eligibility', err),
//       });
//     }
//   }

//   // If a product only has one size (or one color) across all its variants,
//   // there's nothing to actually choose - auto-select it so the customer
//   // isn't stuck clicking a "pick one" button with only one option.
//   private preselectSingleOptionVariants(product: ProductResponse): void {
//     const sizes = Array.from(new Set((product.variants ?? []).map(v => v.size).filter((s): s is string => !!s)));
//     const colors = Array.from(new Set((product.variants ?? []).map(v => v.color).filter((c): c is string => !!c)));
//     if (sizes.length === 1) this.selectedSize.set(sizes[0]);
//     if (colors.length === 1) this.selectedColor.set(colors[0]);
//   }

//   private loadReviews(productId: number): void {
//     this.reviewsLoading.set(true);
//     this.reviewService.getReviews(productId).subscribe({
//       next: (res) => {
//         this.reviews.set(res.data || []);
//         this.reviewsLoading.set(false);
//       },
//       error: (err) => {
//         console.error('Failed to load reviews', err);
//         this.reviewsLoading.set(false);
//       }
//     });
//   }

//   /**
//    * Real per-product SEO - each product now has its own URL (/products/:id)
//    * and its own <title>/meta description, instead of every product sharing
//    * one generic listing-page title. This is what actually makes individual
//    * products discoverable/indexable, not just the storefront as a whole.
//    */
//   private setSeoTags(product: ProductResponse): void {
//     const title = `${product.name} - Buy Online`;
//     const description = product.description?.slice(0, 155) ||
//       `Shop ${product.name} at the best price. Fast shipping, easy returns.`;

//     this.titleService.setTitle(title);
//     this.metaService.updateTag({ name: 'description', content: description });
//     this.metaService.updateTag({ property: 'og:title', content: title });
//     this.metaService.updateTag({ property: 'og:description', content: description });
//     this.metaService.updateTag({ property: 'og:image', content: productImageUrl(product.imageUrl) });
//     this.metaService.updateTag({ property: 'og:type', content: 'product' });
//   }

//   isInWishlist(productId: number): boolean {
//     return this.wishlistService.isWishlisted(productId);
//   }

//   toggleWishlist(productId: number): void {
//     if (!this.authService.isLoggedIn()) {
//       this.toastService.error('Please login first to use your wishlist.');
//       return;
//     }
//     this.wishlistService.toggle(productId).subscribe({
//       next: (res) => this.toastService.success(res.data ? 'Added to wishlist.' : 'Removed from wishlist.'),
//       error: () => this.toastService.error('Could not update wishlist.'),
//     });
//   }

//   selectSize(size: string): void {
//     this.selectedSize.set(size);
//     this.quantity.set(1);
//   }

//   selectColor(color: string): void {
//     this.selectedColor.set(color);
//     this.quantity.set(1);
//   }

//   changeQuantity(delta: number): void {
//     const next = this.quantity() + delta;
//     const stock = this.effectiveStock();
//     if (next >= 1 && next <= stock) {
//       this.quantity.set(next);
//     }
//   }

//   addToCart(): void {
//     const product = this.product();
//     if (!product) return;

//     if (this.hasVariants() && !this.selectedVariant()) {
//       this.toastService.error('Please select a size/color first.');
//       return;
//     }

//     const variantId = this.selectedVariant()?.variantId ?? null;

//     this.cartService.addItemToCart(product.productId, this.quantity(), variantId).subscribe({
//       next: () => this.toastService.success('Added to your cart!'),
//       error: (err) => {
//         if (err.status === 401) {
//           this.toastService.error('Please login first to add items to the cart.');
//         } else {
//           this.toastService.error(err.error?.message || 'Could not add item to cart.');
//         }
//       }
//     });
//   }

//   // --- Review submission ---
//   setReviewRating(stars: number): void {
//     this.newReviewRating.set(stars);
//   }

//   submitReview(): void {
//     const product = this.product();
//     if (!product) return;

//     if (this.newReviewRating() < 1) {
//       this.toastService.error('Please choose a star rating.');
//       return;
//     }

//     this.submittingReview.set(true);
//     this.reviewService.submitReview(product.productId, {
//       rating: this.newReviewRating(),
//       comment: this.newReviewComment.trim() || null,
//     }).subscribe({
//       next: (res) => {
//         this.submittingReview.set(false);
//         this.showReviewForm.set(false);
//         this.newReviewRating.set(0);
//         this.newReviewComment = '';
//         if (res.data) {
//           this.reviews.update(list => [res.data as ReviewResponse, ...list]);
//         }
//         this.eligibility.update(e => e ? { ...e, canReview: false, alreadyReviewed: true } : e);
//         this.toastService.success('Thanks for your review!');
//       },
//       error: (err) => {
//         this.submittingReview.set(false);
//         this.toastService.error(err.error?.message || 'Could not submit your review.');
//       }
//     });
//   }
// }


import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';
import { ProductService } from '../../Services/product-service';
import { CartService } from '../../Services/cart-service';
import { WishlistService } from '../../Services/wishlist-service';
import { AuthService } from '../../Services/auth-service';
import { ReviewService } from '../../Services/review-service';
import { ToastService } from '../../shared/toast/toast-service';
import { ProductResponse, ProductVariantResponse } from '../../Models/Product.model';
import { ReviewResponse, ReviewEligibilityResponse } from '../../Models/Review.model';
import { productImageUrl } from '../../shared/api.config';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, FormsModule, RouterLink],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private wishlistService = inject(WishlistService);
  private authService = inject(AuthService);
  private reviewService = inject(ReviewService);
  private toastService = inject(ToastService);
  private titleService = inject(Title);
  private metaService = inject(Meta);
  private router = inject(Router);

  protected readonly productImageUrl = productImageUrl;

  product = signal<ProductResponse | null>(null);
  loading = signal(true);
  notFound = signal(false);
  loadError = signal(false); // <--- ADDED: Fixes TS2339 error
  quantity = signal(1);

  // --- Variants ---
  selectedSize = signal<string | null>(null);
  selectedColor = signal<string | null>(null);

  hasVariants = computed(() => (this.product()?.variants?.length ?? 0) > 0);

  availableSizes = computed(() => {
    const sizes = (this.product()?.variants ?? [])
      .map(v => v.size)
      .filter((s): s is string => !!s);
    return Array.from(new Set(sizes));
  });

  availableColors = computed(() => {
    const colors = (this.product()?.variants ?? [])
      .map(v => v.color)
      .filter((c): c is string => !!c);
    return Array.from(new Set(colors));
  });

  selectedVariant = computed<ProductVariantResponse | null>(() => {
    const variants = this.product()?.variants ?? [];
    if (variants.length === 0) return null;

    const needsSize = this.availableSizes().length > 0;
    const needsColor = this.availableColors().length > 0;
    if (needsSize && !this.selectedSize()) return null;
    if (needsColor && !this.selectedColor()) return null;

    return variants.find(v =>
      (!needsSize || v.size === this.selectedSize()) &&
      (!needsColor || v.color === this.selectedColor())
    ) ?? null;
  });

  effectiveStock = computed(() => {
    const product = this.product();
    if (!product) return 0;
    if (!this.hasVariants()) return product.stock;
    return this.selectedVariant()?.stock ?? 0;
  });

  effectivePrice = computed(() => {
    const product = this.product();
    if (!product) return 0;
    const override = this.hasVariants() ? this.selectedVariant()?.priceOverride : null;
    const base = override ?? product.price;
    if (!product.discountPercentage) return base;
    return base * (1 - product.discountPercentage / 100);
  });

  // --- Reviews ---
  reviews = signal<ReviewResponse[]>([]);
  reviewsLoading = signal(true);
  eligibility = signal<ReviewEligibilityResponse | null>(null);
  showReviewForm = signal(false);
  submittingReview = signal(false);
  newReviewRating = signal(0);
  newReviewComment = '';

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const productId = idParam ? Number(idParam) : NaN;

    if (!idParam || isNaN(productId)) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    this.productService.getProductById(productId).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.product.set(res.data);
          this.setSeoTags(res.data);
          this.preselectSingleOptionVariants(res.data);
        } else {
          this.notFound.set(true);
        }
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
        } else {
          this.loadError.set(true); // Now successfully references the signal property above
        }
      }
    });

    this.loadReviews(productId);

    if (this.authService.isLoggedIn()) {
      this.reviewService.checkEligibility(productId).subscribe({
        next: (res) => this.eligibility.set(res.data ?? null),
        error: (err) => console.error('Failed to check review eligibility', err),
      });
    }
  }

  private preselectSingleOptionVariants(product: ProductResponse): void {
    const sizes = Array.from(new Set((product.variants ?? []).map(v => v.size).filter((s): s is string => !!s)));
    const colors = Array.from(new Set((product.variants ?? []).map(v => v.color).filter((c): c is string => !!c)));
    if (sizes.length === 1) this.selectedSize.set(sizes[0]);
    if (colors.length === 1) this.selectedColor.set(colors[0]);
  }

  private loadReviews(productId: number): void {
    this.reviewsLoading.set(true);
    this.reviewService.getReviews(productId).subscribe({
      next: (res) => {
        this.reviews.set(res.data || []);
        this.reviewsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load reviews', err);
        this.reviewsLoading.set(false);
      }
    });
  }

  private setSeoTags(product: ProductResponse): void {
    const title = `${product.name} - Buy Online`;
    const description = product.description?.slice(0, 155) ||
      `Shop ${product.name} at the best price. Fast shipping, easy returns.`;

    this.titleService.setTitle(title);
    this.metaService.updateTag({ name: 'description', content: description });
    this.metaService.updateTag({ property: 'og:title', content: title });
    this.metaService.updateTag({ property: 'og:description', content: description });
    this.metaService.updateTag({ property: 'og:image', content: productImageUrl(product.imageUrl) });
    this.metaService.updateTag({ property: 'og:type', content: 'product' });
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistService.isWishlisted(productId);
  }

  toggleWishlist(productId: number): void {
    if (!this.authService.isLoggedIn()) {
      this.toastService.error('Please login first to use your wishlist.');
      return;
    }
    this.wishlistService.toggle(productId).subscribe({
      next: (res) => this.toastService.success(res.data ? 'Added to wishlist.' : 'Removed from wishlist.'),
      error: () => this.toastService.error('Could not update wishlist.'),
    });
  }

  selectSize(size: string): void {
    this.selectedSize.set(size);
    this.quantity.set(1);
  }

  selectColor(color: string): void {
    this.selectedColor.set(color);
    this.quantity.set(1);
  }

  changeQuantity(delta: number): void {
    const next = this.quantity() + delta;
    const stock = this.effectiveStock();
    if (next >= 1 && next <= stock) {
      this.quantity.set(next);
    }
  }

  addToCart(): void {
    const product = this.product();
    if (!product) return;

    if (this.hasVariants() && !this.selectedVariant()) {
      this.toastService.error('Please select a size/color first.');
      return;
    }

    const variantId = this.selectedVariant()?.variantId ?? null;

    this.cartService.addItemToCart(product.productId, this.quantity(), variantId).subscribe({
      next: () => this.toastService.success('Added to your cart!'),
      error: (err) => {
        if (err.status === 401) {
          this.toastService.error('Please login first to add items to the cart.');
        } else {
          this.toastService.error(err.error?.message || 'Could not add item to cart.');
        }
      }
    });
  }

  setReviewRating(stars: number): void {
    this.newReviewRating.set(stars);
  }

  submitReview(): void {
    const product = this.product();
    if (!product) return;

    if (this.newReviewRating() < 1) {
      this.toastService.error('Please choose a star rating.');
      return;
    }

    this.submittingReview.set(true);
    this.reviewService.submitReview(product.productId, {
      rating: this.newReviewRating(),
      comment: this.newReviewComment.trim() || null,
    }).subscribe({
      next: (res) => {
        this.submittingReview.set(false);
        this.showReviewForm.set(false);
        this.newReviewRating.set(0);
        this.newReviewComment = '';
        if (res.data) {
          this.reviews.update(list => [res.data as ReviewResponse, ...list]);
        }
        this.eligibility.update(e => e ? { ...e, canReview: false, alreadyReviewed: true } : e);
        this.toastService.success('Thanks for your review!');
      },
      error: (err) => {
        this.submittingReview.set(false);
        this.toastService.error(err.error?.message || 'Could not submit your review.');
      }
    });
  }
  buyNow(): void {
    const product = this.product();
    if (!product) return;

    if (this.hasVariants() && !this.selectedVariant()) {
      this.toastService.error('Please select a size/color first.');
      return;
    }

    // Navigate directly to checkout, bypassing cart addition
    this.router.navigate(['/checkout'], {
      queryParams: {
        buyNow: product.productId,
        quantity: this.quantity(),
        variantId: this.selectedVariant()?.variantId ?? null
      }
    });
  }
}