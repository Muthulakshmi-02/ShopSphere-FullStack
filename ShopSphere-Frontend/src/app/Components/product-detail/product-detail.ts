import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';
import { MatIconModule } from '@angular/material/icon';
import { ProductService } from '../../Services/product-service';
import { CartService } from '../../Services/cart-service';
import { WishlistService } from '../../Services/wishlist-service';
import { AuthService } from '../../Services/auth-service';
import { ReviewService } from '../../Services/review-service';
import { QuestionService } from '../../Services/question-service';
import { ToastService } from '../../shared/toast/toast-service';
import { ConfirmService } from '../../shared/confirm-modal/confirm-service';
import { ProductResponse, ProductVariantResponse } from '../../Models/Product.model';
import { ReviewResponse, ReviewEligibilityResponse } from '../../Models/Review.model';
import { ProductQuestionResponse } from '../../Models/Question.model';
import { productImageUrl } from '../../shared/api.config';

// The server refuses a quantity above 99 (the cart and the order both enforce it).
const MAX_QUANTITY = 99;

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe, FormsModule, RouterLink, MatIconModule],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private wishlistService = inject(WishlistService);
  protected auth = inject(AuthService);
  protected authService = this.auth;
  private reviewService = inject(ReviewService);
  private questionService = inject(QuestionService);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmService);
  private titleService = inject(Title);
  private metaService = inject(Meta);
  private router = inject(Router);

  protected readonly productImageUrl = productImageUrl;

  product = signal<ProductResponse | null>(null);
  loading = signal(true);
  notFound = signal(false);
  loadError = signal(false);
  quantity = signal(1);

  // --- Image Gallery ---
  selectedImageIndex = signal<number>(0);

  galleryImages = computed<string[]>(() => {
    const p = this.product();
    if (!p) return [];
    if (p.imageUrls && p.imageUrls.length > 0) {
      return p.imageUrls;
    }
    return p.imageUrl ? [p.imageUrl] : [];
  });

  activeImageUrl = computed<string>(() => {
    const list = this.galleryImages();
    if (list.length === 0) return '';
    const idx = this.selectedImageIndex();
    return list[idx] || list[0];
  });

  selectImage(index: number): void {
    this.selectedImageIndex.set(index);
  }

  prevImage(): void {
    const total = this.galleryImages().length;
    if (total <= 1) return;
    this.selectedImageIndex.update(i => (i - 1 + total) % total);
  }

  nextImage(): void {
    const total = this.galleryImages().length;
    if (total <= 1) return;
    this.selectedImageIndex.update(i => (i + 1) % total);
  }

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

  // The price before any discount: the chosen option's own price if it has one.
  basePrice = computed(() => {
    const product = this.product();
    if (!product) return 0;
    const override = this.hasVariants() ? this.selectedVariant()?.priceOverride : null;
    return override ?? product.price;
  });

  // CHANGED: rounded to paise (half up), the way the server rounds, so this page can never show a
  // different amount than the one charged.
  effectivePrice = computed(() => {
    const product = this.product();
    if (!product) return 0;
    const base = this.basePrice();
    if (!product.discountPercentage) return base;
    const value = base - (base * product.discountPercentage) / 100;
    return Math.round((value + Number.EPSILON) * 100) / 100;
  });

  // --- Reviews ---
  reviews = signal<ReviewResponse[]>([]);
  reviewsLoading = signal(true);
  eligibility = signal<ReviewEligibilityResponse | null>(null);
  showReviewForm = signal(false);
  submittingReview = signal(false);
  newReviewRating = signal(0);
  newReviewComment = '';

  // --- Product Q&A ---
  questions = signal<ProductQuestionResponse[]>([]);
  questionsLoading = signal(true);
  questionSearchQuery = signal('');
  showQuestionForm = signal(false);
  newQuestionText = '';
  submittingQuestion = signal(false);

  // Admin answering
  answeringQuestionId = signal<number | null>(null);
  answerText = '';
  submittingAnswer = signal(false);
  deletingQuestionId = signal<number | null>(null);

  filteredQuestions = computed(() => {
    const q = this.questionSearchQuery().trim().toLowerCase();
    if (!q) return this.questions();
    return this.questions().filter(item =>
      item.question.toLowerCase().includes(q) ||
      (item.answer && item.answer.toLowerCase().includes(q)) ||
      item.askerName.toLowerCase().includes(q)
    );
  });

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
          this.loadError.set(true);
        }
      }
    });

    this.loadReviews(productId);
    this.loadQuestions(productId);

    if (this.authService.isLoggedIn() && !this.authService.isAdmin()) {
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

  // ---------- Who may shop ----------
  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  // The server only lets customers buy (cart, checkout, wishlist). Tell an admin why nothing happens.
  private blockedForAdmin(): boolean {
    if (this.authService.isLoggedIn() && this.authService.isAdmin()) {
      this.toastService.info('You are signed in as an administrator. Sign in with a customer account to shop.');
      return true;
    }
    return false;
  }

  private sendToLogin(message: string): void {
    this.toastService.info(message);
    this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
  }

  isInWishlist(productId: number): boolean {
    return this.wishlistService.isWishlisted(productId);
  }

  toggleWishlist(productId: number): void {
    // CHANGED: a guest is sent to sign in and brought back here (it was only an error message).
    if (!this.authService.isLoggedIn()) {
      this.sendToLogin('Please sign in to use your wishlist.');
      return;
    }
    if (this.blockedForAdmin()) return;

    this.wishlistService.toggle(productId).subscribe({
      next: (res) => this.toastService.success(res.data ? 'Added to wishlist.' : 'Removed from wishlist.'),
      error: () => this.toastService.error('Could not update wishlist.'),
    });
  }

  // ---------- Options ----------
  // NEW: a size (or colour) that has no stock in ANY variant is greyed out and cannot be picked,
  // instead of letting the customer choose it and only then see "Out of stock".
  isSizeSoldOut(size: string): boolean {
    const matching = (this.product()?.variants ?? []).filter(v => v.size === size);
    return matching.length > 0 && matching.every(v => v.stock <= 0);
  }

  isColorSoldOut(color: string): boolean {
    const matching = (this.product()?.variants ?? []).filter(v => v.color === color);
    return matching.length > 0 && matching.every(v => v.stock <= 0);
  }

  selectSize(size: string): void {
    if (this.isSizeSoldOut(size)) return;
    this.selectedSize.set(size);
    this.quantity.set(1);
  }

  selectColor(color: string): void {
    if (this.isColorSoldOut(color)) return;
    this.selectedColor.set(color);
    this.quantity.set(1);
  }

  changeQuantity(delta: number): void {
    const next = this.quantity() + delta;
    const max = Math.min(this.effectiveStock(), MAX_QUANTITY);
    if (next >= 1 && next <= max) {
      this.quantity.set(next);
    }
  }

  addToCart(): void {
    const product = this.product();
    if (!product) return;

    // CHANGED: guests are sent to sign in and brought back to this product.
    if (!this.authService.isLoggedIn()) {
      this.sendToLogin('Please sign in to add items to your cart.');
      return;
    }
    if (this.blockedForAdmin()) return;

    if (this.hasVariants() && !this.selectedVariant()) {
      this.toastService.error('Please select a size/color first.');
      return;
    }

    const variantId = this.selectedVariant()?.variantId ?? null;

    this.cartService.addItemToCart(product.productId, this.quantity(), variantId).subscribe({
      next: () => this.toastService.success('Added to your cart!'),
      error: (err) => {
        this.toastService.error(err.error?.message || 'Could not add item to cart.');
      }
    });
  }

  buyNow(): void {
    const product = this.product();
    if (!product) return;

    if (this.blockedForAdmin()) return;

    if (this.hasVariants() && !this.selectedVariant()) {
      this.toastService.error('Please select a size/color first.');
      return;
    }

    // Straight to checkout, bypassing the cart. A guest is sent to sign in by the route guard
    // and comes back to checkout afterwards.
    this.router.navigate(['/checkout'], {
      queryParams: {
        buyNow: product.productId,
        quantity: this.quantity(),
        variantId: this.selectedVariant()?.variantId ?? null
      }
    });
  }

  // ---------- Reviews ----------
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

  // ---------- Q&A ----------
  loadQuestions(productId: number): void {
    this.questionsLoading.set(true);
    this.questionService.getQuestions(productId).subscribe({
      next: (res) => {
        this.questions.set(res.data || []);
        this.questionsLoading.set(false);
      },
      error: () => {
        this.questionsLoading.set(false);
      }
    });
  }

  askQuestion(): void {
    const p = this.product();
    if (!p) return;

    const trimmed = this.newQuestionText.trim();
    if (trimmed.length < 5) {
      this.toastService.error('Please enter a question with at least 5 characters.');
      return;
    }

    this.submittingQuestion.set(true);
    this.questionService.askQuestion(p.productId, { question: trimmed }).subscribe({
      next: (res) => {
        this.submittingQuestion.set(false);
        this.showQuestionForm.set(false);
        this.newQuestionText = '';
        if (res.data) {
          this.questions.update(list => [res.data, ...list]);
        }
        this.toastService.success('Your question has been posted!');
      },
      error: (err) => {
        this.submittingQuestion.set(false);
        this.toastService.error(err.error?.message || 'Could not post question.');
      }
    });
  }

  openAnswerForm(q: ProductQuestionResponse): void {
    this.answeringQuestionId.set(q.id);
    this.answerText = q.answer || '';
  }

  cancelAnswer(): void {
    this.answeringQuestionId.set(null);
    this.answerText = '';
  }

  submitAnswer(questionId: number): void {
    const trimmed = this.answerText.trim();
    if (trimmed.length < 2) {
      this.toastService.error('Answer must contain at least 2 characters.');
      return;
    }

    this.submittingAnswer.set(true);
    this.questionService.answerQuestion(questionId, { answer: trimmed }).subscribe({
      next: (res) => {
        this.submittingAnswer.set(false);
        this.answeringQuestionId.set(null);
        this.answerText = '';
        if (res.data) {
          const updated = res.data;
          this.questions.update(list => list.map(q => q.id === questionId ? updated : q));
        }
        this.toastService.success('Answer published successfully.');
      },
      error: (err) => {
        this.submittingAnswer.set(false);
        this.toastService.error(err.error?.message || 'Could not publish answer.');
      }
    });
  }

  async deleteQuestion(q: ProductQuestionResponse): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: 'Delete Question?',
      message: `Are you sure you want to permanently remove this question: "${q.question}"?`,
      confirmLabel: 'Delete Question',
      danger: true
    });
    if (!confirmed) return;

    this.deletingQuestionId.set(q.id);
    this.questionService.deleteQuestion(q.id).subscribe({
      next: () => {
        this.deletingQuestionId.set(null);
        this.questions.update(list => list.filter(item => item.id !== q.id));
        this.toastService.success('Question deleted.');
      },
      error: (err) => {
        this.deletingQuestionId.set(null);
        this.toastService.error(err.error?.message || 'Could not delete question.');
      }
    });
  }
}
