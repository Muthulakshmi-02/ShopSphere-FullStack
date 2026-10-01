import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../Services/order-service';

interface TickerSale {
  name: string;
  item: string;
}

/**
 * Signature "real-time" element for the storefront - a scrolling feed of
 * ACTUAL recent purchases, pulled from real orders via the public
 * GET /api/orders/recent-activity endpoint. No illustrative/fake data -
 * if there's no real purchase history yet, the ticker simply doesn't
 * render rather than showing made-up activity.
 */
@Component({
  selector: 'app-live-ticker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ticker-bar" *ngIf="loopedSales().length > 0">
      <div class="ticker-track">
        <span class="ticker-item" *ngFor="let s of loopedSales()">
          <span class="zn-live-dot"></span>
          <b>{{ s.name }}</b> just bought <b>{{ s.item }}</b>
        </span>
      </div>
    </div>
  `,
  styles: [`
    .ticker-bar {
      background: var(--zn-surface, #161D2E);
      border-bottom: 1px solid var(--zn-border, rgba(245,245,247,0.08));
      overflow: hidden;
      white-space: nowrap;
      height: 36px;
      display: flex;
      align-items: center;
    }
    .ticker-track {
      display: inline-flex;
      align-items: center;
      animation: ticker-scroll 32s linear infinite;
    }
    .ticker-item {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 0 26px;
      font-size: 12px;
      color: var(--zn-text-muted, #8B92A8);
      border-right: 1px solid var(--zn-border, rgba(245,245,247,0.08));
      font-family: var(--zn-font-body, inherit);
    }
    .ticker-item b {
      color: var(--zn-text-main, #F5F5F7);
      font-weight: 600;
    }
    @keyframes ticker-scroll {
      0% { transform: translateX(0); }
      100% { transform: translateX(-50%); }
    }
  `]
})
export class LiveTicker implements OnInit {
  private orderService = inject(OrderService);

  private sales = signal<TickerSale[]>([]);

  // Duplicated once so the CSS scroll animation loops seamlessly - this is
  // just a visual trick for the marquee effect, not the dedup the buyer
  // requested (that dedup already happened server-side, by buyer+product).
  loopedSales = signal<TickerSale[]>([]);

  ngOnInit(): void {
    this.orderService.getRecentActivity().subscribe({
      next: (res) => {
        const activity = (res?.data || []) as { buyerName: string; productName: string }[];
        const sales: TickerSale[] = activity.map(a => ({ name: a.buyerName, item: a.productName }));
        this.sales.set(sales);
        this.loopedSales.set(sales.length > 0 ? [...sales, ...sales] : []);
      },
      error: (err) => {
        console.error('Failed to load recent activity for live ticker', err);
        // No fallback to fake data - if the real feed fails, the ticker
        // just doesn't show, rather than displaying made-up purchases.
        this.loopedSales.set([]);
      }
    });
  }
}
