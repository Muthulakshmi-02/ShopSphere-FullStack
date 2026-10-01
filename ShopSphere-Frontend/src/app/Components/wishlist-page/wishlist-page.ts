import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { WishlistService, WishlistItemResponse } from '../../Services/wishlist-service';
import { CartService } from '../../Services/cart-service';
import { AuthService } from '../../Services/auth-service';
import { ToastService } from '../../shared/toast/toast-service';
import { ConfirmService } from '../../shared/confirm-modal/confirm-service';
import { productImageUrl } from '../../shared/api.config';
import { ProductResponse } from '../../Models/Product.model';

@Component({
  selector: 'app-wishlist-page',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink],
  templateUrl: './wishlist-page.html',
  styleUrls: ['./wishlist-page.css']
})
export class WishlistPage implements OnInit {
  private wishlistService = inject(WishlistService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private confirmService = inject(ConfirmService);
  private router = inject(Router);

  protected readonly productImageUrl = productImageUrl;

  items = signal<WishlistItemResponse[]>([]);
  loading = signal(true);
  removingId = signal<number | null>(null);

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.loading.set(false);
      return;
    }

    this.wishlistService.getMyWishlist().subscribe({
      next: (res) => {
        this.items.set(res.data || []);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load wishlist', err);
        this.loading.set(false);
        this.toastService.error('Could not load your wishlist.');
      }
    });
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  discountedPrice(product: ProductResponse): number {
    if (!product.discountPercentage) return product.price;
    return product.price * (1 - product.discountPercentage / 100);
  }

  addToCart(product: ProductResponse): void {
    if (product.variants && product.variants.length > 0) {
      this.toastService.info('This product has options - pick one on the product page.');
      this.router.navigate(['/products', product.productId]);
      return;
    }

    this.cartService.addItemToCart(product.productId, 1).subscribe({
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

  async remove(item: WishlistItemResponse): Promise<void> {
    const confirmed = await this.confirmService.ask({
      title: 'Remove from wishlist?',
      message: `Remove "${item.product.name}" from your wishlist.`,
      confirmLabel: 'Remove',
      danger: true,
    });
    if (!confirmed) return;

    this.removingId.set(item.product.productId);
    this.wishlistService.toggle(item.product.productId).subscribe({
      next: () => {
        this.removingId.set(null);
        this.items.update(list => list.filter(i => i.wishlistItemId !== item.wishlistItemId));
        this.toastService.success('Removed from wishlist.');
      },
      error: () => {
        this.removingId.set(null);
        this.toastService.error('Could not remove item.');
      }
    });
  }
}
