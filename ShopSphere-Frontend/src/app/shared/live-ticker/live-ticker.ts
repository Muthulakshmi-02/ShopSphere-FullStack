import { Component, OnDestroy, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { OrderService } from '../../Services/order-service';

interface TickerSale {
  name: string;
  item: string;
}

/**
 * Scrolling feed of ACTUAL recent purchases from the public GET /api/orders/recent-activity.
 * No made-up data: with no purchase history (or if the request fails) the ticker is hidden.
 * Refreshes every minute; it used to load once and then never change.
 */
@Component({
  selector: 'app-live-ticker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ticker-bar" *ngIf="sales().length > 0" aria-label="Recent purchases">
      <div class="ticker-track">
        <span class="ticker-item" *ngFor="let s of sales()">
          <span class="zn-live-dot"></span>
          <b>{{ s.name }}</b> just bought <b>{{ s.item }}</b>
        </span>
        <!-- Second copy so the scroll loops seamlessly; hidden from screen readers so every
             purchase is not announced twice. -->
        <span class="ticker-item" aria-hidden="true" *ngFor="let s of sales()">
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
    .ticker-bar:hover .ticker-track {
      animation-play-state: paused;
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
    /* People who ask their device for less motion get a still ticker. */
    @media (prefers-reduced-motion: reduce) {
      .ticker-track { animation: none; }
    }
  `]
})
export class LiveTicker implements OnInit, OnDestroy {
  private orderService = inject(OrderService);
  private platformId = inject(PLATFORM_ID);

  sales = signal<TickerSale[]>([]);

  private timer: ReturnType<typeof setInterval> | null = null;
  private lastKey = '';

  ngOnInit(): void {
    this.load();
    if (isPlatformBrowser(this.platformId)) {
      this.timer = setInterval(() => this.load(), 60_000);
    }
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  private load(): void {
    this.orderService.getRecentActivity().subscribe({
      next: (res) => {
        const activity = (res?.data || []) as { buyerName: string; productName: string }[];
        const sales: TickerSale[] = activity.map(a => ({ name: a.buyerName, item: a.productName }));

        // Only touch the screen when something actually changed (no flicker every minute).
        const key = JSON.stringify(sales);
        if (key !== this.lastKey) {
          this.lastKey = key;
          this.sales.set(sales);
        }
      },
      error: () => {
        // No fallback to fake data: if the real feed fails the ticker just stays as it is / hidden.
        if (this.sales().length === 0) this.sales.set([]);
      }
    });
  }
}