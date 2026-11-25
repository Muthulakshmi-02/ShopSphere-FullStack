import { Component, Input, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { OrderStatusHistoryResponse } from '../../Models/Order.model';

interface TimelineStep {
  key: string;
  label: string;
}

const STEPS: TimelineStep[] = [
  { key: 'PENDING', label: 'Order placed' },
  { key: 'CONFIRMED', label: 'Confirmed' },
  { key: 'SHIPPED', label: 'Shipped' },
  { key: 'DELIVERED', label: 'Delivered' },
];

/**
 * Visual order-status tracker for customers with real milestone timestamps,
 * courier tracking information, and detailed event history.
 */
@Component({
  selector: 'app-order-timeline',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="timeline-container">
      <div class="order-timeline" role="list" [attr.aria-label]="'Order status: ' + currentLabel"
        *ngIf="status !== 'CANCELLED'; else cancelledState">
        <div
          class="timeline-step"
          role="listitem"
          *ngFor="let step of steps; let i = index"
          [class.done]="i <= currentIndex"
          [class.current]="i === currentIndex"
          [class.line-done]="i < currentIndex"
          [attr.aria-current]="i === currentIndex ? 'step' : null"
        >
          <div class="step-dot">
            <svg *ngIf="isComplete(i)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <path d="M5 13l4 4L19 7" />
            </svg>
            <span *ngIf="!isComplete(i)">{{ i + 1 }}</span>
          </div>
          <span class="step-label">{{ labelFor(step, i) }}</span>
          <span class="step-time" *ngIf="timestampFor(step.key)">
            {{ timestampFor(step.key) | date:'dd MMM, hh:mm a' }}
          </span>
          <div class="step-line" *ngIf="i < steps.length - 1"></div>
        </div>
      </div>

      <ng-template #cancelledState>
        <div class="order-timeline cancelled">
          <div class="cancelled-icon">✕</div>
          <div class="cancelled-info">
            <span class="cancelled-label">{{ cancelledText }}</span>
            <span class="step-time" *ngIf="timestampFor('CANCELLED')">
              {{ timestampFor('CANCELLED') | date:'dd MMM, hh:mm a' }}
            </span>
          </div>
        </div>
      </ng-template>

      <!-- Package Tracking Info Bar (when Shipped or Delivered) -->
      <div class="tracking-infobar" *ngIf="carrier || trackingNumber || estimatedDelivery">
        <div class="tracking-item" *ngIf="carrier">
          <span class="tracking-tag">Courier</span>
          <strong>{{ carrier }}</strong>
        </div>
        <div class="tracking-item" *ngIf="trackingNumber">
          <span class="tracking-tag">Tracking ID</span>
          <code class="tracking-num">{{ trackingNumber }}</code>
        </div>
        <div class="tracking-item" *ngIf="estimatedDelivery && status !== 'DELIVERED' && status !== 'CANCELLED'">
          <span class="tracking-tag">Est. Delivery</span>
          <strong class="est-date">{{ estimatedDelivery | date:'EEE, dd MMM yyyy' }}</strong>
        </div>
      </div>

      <!-- Expandable Tracking History Log -->
      <div class="history-toggle-row" *ngIf="statusHistory && statusHistory.length > 0">
        <button type="button" class="history-toggle-btn" (click)="toggleHistory()">
          <span>{{ showHistory() ? 'Hide' : 'View' }} Tracking Milestones ({{ statusHistory.length }})</span>
          <span class="chevron" [class.open]="showHistory()">▾</span>
        </button>
      </div>

      <div class="history-log" *ngIf="showHistory() && statusHistory && statusHistory.length > 0">
        <div class="history-event" *ngFor="let ev of statusHistory">
          <div class="event-bullet" [class.cancelled]="ev.status === 'CANCELLED'"></div>
          <div class="event-content">
            <div class="event-header">
              <span class="event-status">{{ ev.status }}</span>
              <span class="event-date">{{ ev.changedAt | date:'dd MMM yyyy, hh:mm a' }}</span>
            </div>
            <p class="event-note">{{ ev.note }}</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .timeline-container {
      width: 100%;
    }
    .order-timeline {
      display: flex;
      align-items: flex-start;
      width: 100%;
      padding: 1rem 0;
    }
    .timeline-step {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      position: relative;
      text-align: center;
    }
    .step-dot {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.08);
      color: #8B92A8;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.82rem;
      font-weight: 700;
      z-index: 1;
      transition: all 0.25s ease;
    }
    .step-dot svg { width: 15px; height: 15px; }
    .timeline-step.done .step-dot {
      background: #FF3D71;
      color: #fff;
      box-shadow: 0 0 12px rgba(255, 61, 113, 0.4);
    }
    .timeline-step.current .step-dot {
      background: #FF3D71;
      color: #fff;
      box-shadow: 0 0 0 4px rgba(255, 61, 113, 0.2);
    }
    .step-label {
      margin-top: 0.5rem;
      font-size: 0.8rem;
      color: #8B92A8;
      font-weight: 500;
    }
    .timeline-step.done .step-label {
      color: #F5F5F7;
      font-weight: 700;
    }
    .step-time {
      font-size: 0.72rem;
      color: #8B92A8;
      margin-top: 2px;
    }
    .step-line {
      position: absolute;
      top: 16px;
      left: 50%;
      width: 100%;
      height: 2px;
      background: rgba(245, 245, 247, 0.12);
      z-index: 0;
    }
    .timeline-step.line-done .step-line {
      background: #FF3D71;
    }

    .order-timeline.cancelled {
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      padding: 1rem;
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.2);
      border-radius: 12px;
    }
    .cancelled-icon {
      width: 30px;
      height: 30px;
      flex-shrink: 0;
      border-radius: 50%;
      background: #ef4444;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 1rem;
    }
    .cancelled-info {
      display: flex;
      flex-direction: column;
    }
    .cancelled-label {
      color: #ef4444;
      font-weight: 600;
      font-size: 0.92rem;
    }

    /* Tracking info bar */
    .tracking-infobar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 1.25rem;
      padding: 0.75rem 1rem;
      margin-top: 0.75rem;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(245, 245, 247, 0.08);
      border-radius: 10px;
      font-size: 0.85rem;
    }
    .tracking-item {
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }
    .tracking-tag {
      color: #8B92A8;
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    .tracking-num {
      background: rgba(255, 61, 113, 0.1);
      color: #FF3D71;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: monospace;
      font-weight: 600;
    }
    .est-date {
      color: #22c55e;
    }

    /* History toggle & log */
    .history-toggle-row {
      display: flex;
      justify-content: flex-end;
      margin-top: 0.5rem;
    }
    .history-toggle-btn {
      background: none;
      border: none;
      color: #8B92A8;
      font-size: 0.78rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 2px 6px;
      border-radius: 4px;
      transition: color 0.15s;
    }
    .history-toggle-btn:hover {
      color: #FF3D71;
    }
    .chevron {
      display: inline-block;
      transition: transform 0.2s;
    }
    .chevron.open {
      transform: rotate(180deg);
    }
    .history-log {
      margin-top: 0.75rem;
      padding: 0.85rem 1rem;
      background: rgba(0, 0, 0, 0.3);
      border-radius: 10px;
      border-left: 3px solid #FF3D71;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .history-event {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
    }
    .event-bullet {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #FF3D71;
      margin-top: 5px;
      flex-shrink: 0;
    }
    .event-bullet.cancelled {
      background: #ef4444;
    }
    .event-content {
      flex: 1;
    }
    .event-header {
      display: flex;
      justify-content: space-between;
      gap: 0.5rem;
      font-size: 0.78rem;
    }
    .event-status {
      font-weight: 700;
      color: #F5F5F7;
    }
    .event-date {
      color: #8B92A8;
    }
    .event-note {
      margin: 2px 0 0;
      font-size: 0.8rem;
      color: #B8BFD4;
    }
  `]
})
export class OrderTimeline {
  @Input() status: string = 'PENDING';
  @Input() paymentStatus: string | null = null;
  @Input() statusHistory: OrderStatusHistoryResponse[] = [];
  @Input() carrier?: string | null = null;
  @Input() trackingNumber?: string | null = null;
  @Input() estimatedDelivery?: string | null = null;

  showHistory = signal(false);

  steps = STEPS;

  get currentIndex(): number {
    const idx = STEPS.findIndex(s => s.key === this.status);
    return idx === -1 ? 0 : idx;
  }

  isComplete(i: number): boolean {
    return this.status === 'DELIVERED' || i < this.currentIndex;
  }

  labelFor(step: TimelineStep, i: number): string {
    if (i === 0 && this.status === 'PENDING' && this.paymentStatus === 'PENDING') {
      return 'Awaiting payment';
    }
    return step.label;
  }

  get currentLabel(): string {
    return this.labelFor(STEPS[this.currentIndex], this.currentIndex);
  }

  timestampFor(statusKey: string): string | null {
    if (!this.statusHistory || this.statusHistory.length === 0) return null;
    const match = this.statusHistory.find(h => h.status === statusKey);
    return match ? match.changedAt : null;
  }

  toggleHistory(): void {
    this.showHistory.update(v => !v);
  }

  get cancelledText(): string {
    if (this.paymentStatus === 'REFUNDED') return 'This order was cancelled. Your payment was refunded.';
    return this.paymentStatus === 'REFUND_PENDING'
      ? 'This order was cancelled. Your refund is being processed.'
      : 'This order was cancelled';
  }
}
