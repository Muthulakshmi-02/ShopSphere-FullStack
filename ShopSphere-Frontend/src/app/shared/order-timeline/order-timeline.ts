import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

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
 * Visual order-status tracker for customers. Renders the four normal-flow steps as a progress
 * line; a cancelled order shows a distinct state instead of a broken/partial progress bar.
 *
 * Optional: pass [paymentStatus]="order.paymentStatus" (my-orders and order-success pages) so an
 * unpaid card order says "Awaiting payment" and a cancelled-but-paid order says its refund is
 * on the way. Without it everything behaves exactly as before.
 */
@Component({
  selector: 'app-order-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
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
        <div class="step-line" *ngIf="i < steps.length - 1"></div>
      </div>
    </div>

    <ng-template #cancelledState>
      <div class="order-timeline cancelled">
        <div class="cancelled-icon">✕</div>
        <span class="cancelled-label">{{ cancelledText }}</span>
      </div>
    </ng-template>
  `,
  styles: [`
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
      width: 30px;
      height: 30px;
      border-radius: 50%;
      background: var(--zn-border, #e2e8f0);
      color: var(--zn-text-muted, #64748b);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: 700;
      z-index: 1;
      transition: background 0.25s ease, color 0.25s ease;
    }
    .step-dot svg { width: 14px; height: 14px; }
    .timeline-step.done .step-dot {
      background: var(--zn-primary, #6366f1);
      color: #fff;
    }
    .timeline-step.current .step-dot {
      box-shadow: 0 0 0 4px var(--zn-primary-light, #eef2ff);
    }
    .step-label {
      margin-top: 0.5rem;
      font-size: 0.78rem;
      color: var(--zn-text-muted, #64748b);
      font-weight: 500;
    }
    .timeline-step.done .step-label {
      color: var(--zn-text-main, #1e293b);
      font-weight: 700;
    }
    .step-line {
      position: absolute;
      top: 15px;
      left: 50%;
      width: 100%;
      height: 2px;
      background: var(--zn-border, #e2e8f0);
      z-index: 0;
    }
    /* The line AFTER the current step stays grey: it used to be coloured as well, which drew
       progress all the way to a step the order had not reached yet. */
    .timeline-step.line-done .step-line {
      background: var(--zn-primary, #6366f1);
    }

    .order-timeline.cancelled {
      align-items: center;
      justify-content: center;
      gap: 0.6rem;
      padding: 1rem;
      background: var(--zn-danger-bg, #fef2f2);
      border-radius: var(--zn-radius-md, 10px);
    }
    .cancelled-icon {
      width: 28px;
      height: 28px;
      flex-shrink: 0;
      border-radius: 50%;
      background: var(--zn-danger, #dc2626);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
    }
    .cancelled-label {
      color: var(--zn-danger, #dc2626);
      font-weight: 600;
      font-size: 0.9rem;
    }
  `]
})
export class OrderTimeline {
  @Input() status: string = 'PENDING';
  @Input() paymentStatus: string | null = null;

  steps = STEPS;

  get currentIndex(): number {
    const idx = STEPS.findIndex(s => s.key === this.status);
    return idx === -1 ? 0 : idx;
  }

  // Delivered is the end: every step, including the last, shows a tick (the last one used to show "4").
  isComplete(i: number): boolean {
    return this.status === 'DELIVERED' || i < this.currentIndex;
  }

  // A still-PENDING order with a PENDING payment is a card order waiting for payment.
  labelFor(step: TimelineStep, i: number): string {
    if (i === 0 && this.status === 'PENDING' && this.paymentStatus === 'PENDING') {
      return 'Awaiting payment';
    }
    return step.label;
  }

  get currentLabel(): string {
    return this.labelFor(STEPS[this.currentIndex], this.currentIndex);
  }

  get cancelledText(): string {
    return this.paymentStatus === 'REFUND_PENDING'
      ? 'This order was cancelled. Your refund is being processed.'
      : 'This order was cancelled';
  }
}