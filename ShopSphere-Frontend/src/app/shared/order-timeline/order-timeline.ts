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
 * Visual order-status tracker for customers. Renders the four normal-flow
 * steps as a progress line; if the order was cancelled, shows a distinct
 * cancelled state instead of a broken/partial progress bar.
 */
@Component({
  selector: 'app-order-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="order-timeline" *ngIf="status !== 'CANCELLED'; else cancelledState">
      <div
        class="timeline-step"
        *ngFor="let step of steps; let i = index"
        [class.done]="i <= currentIndex"
        [class.current]="i === currentIndex"
      >
        <div class="step-dot">
          <svg *ngIf="i < currentIndex" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path d="M5 13l4 4L19 7" />
          </svg>
          <span *ngIf="i >= currentIndex">{{ i + 1 }}</span>
        </div>
        <span class="step-label">{{ step.label }}</span>
        <div class="step-line" *ngIf="i < steps.length - 1"></div>
      </div>
    </div>

    <ng-template #cancelledState>
      <div class="order-timeline cancelled">
        <div class="cancelled-icon">✕</div>
        <span class="cancelled-label">This order was cancelled</span>
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
    .timeline-step.done .step-line {
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

  steps = STEPS;

  get currentIndex(): number {
    const idx = STEPS.findIndex(s => s.key === this.status);
    return idx === -1 ? 0 : idx;
  }
}
