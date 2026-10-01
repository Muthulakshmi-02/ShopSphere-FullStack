import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmService } from './confirm-service';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="confirm-backdrop" *ngIf="confirmService.active() as req" (click)="confirmService.resolve(false)">
      <div class="confirm-card" [attr.data-danger]="req.danger" (click)="$event.stopPropagation()">
        <h3>{{ req.title }}</h3>
        <p>{{ req.message }}</p>
        <div class="confirm-actions">
          <button class="btn-cancel" (click)="confirmService.resolve(false)">
            {{ req.cancelLabel || 'Cancel' }}
          </button>
          <button class="btn-confirm" [attr.data-danger]="req.danger" (click)="confirmService.resolve(true)">
            {{ req.confirmLabel || 'Confirm' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .confirm-backdrop {
      position: fixed;
      inset: 0;
      // background: rgba(15, 23, 42, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 999999;
      padding: var(--zn-space-4, 16px);
      animation: fade-in 150ms ease;
      inset:0;
      background:
    rgba(
8
0,
9
0,
10
0,
11
.65
12
);
    }
    .confirm-card {
      background: var(--zn-surface, #fff);
      border-radius: var(--zn-radius-lg, 16px);
      box-shadow: var(--zn-shadow-lg, 0 12px 32px rgba(15,23,42,0.12));
      padding: var(--zn-space-6, 32px);
      max-width: 400px;
      width: 100%;
    }
    .confirm-card{
    position:relative;
    z-index:1000000;
}
    .confirm-card h3 {
      font-size: 1.15rem;
      margin-bottom: var(--zn-space-2, 8px);
      color: var(--zn-text-main, #1e293b);
    }
    .confirm-card p {
      color: var(--zn-text-muted, #64748b);
      font-size: 0.92rem;
      margin-bottom: var(--zn-space-5, 24px);
      line-height: 1.5;
    }
    .confirm-actions {
      display: flex;
      justify-content: flex-end;
      gap: var(--zn-space-3, 12px);
    }
    .btn-cancel, .btn-confirm {
      padding: 0.6rem 1.2rem;
      border-radius: var(--zn-radius-md, 10px);
      font-weight: 600;
      font-size: 0.9rem;
      cursor: pointer;
      border: none;
      transition: opacity 150ms ease;
    }
    .btn-cancel {
      background: var(--zn-bg-light, #f8fafc);
      color: var(--zn-text-main, #1e293b);
      border: 1px solid var(--zn-border, #e2e8f0);
    }
    .btn-confirm {
      background: var(--zn-primary, #6366f1);
      color: white;
    }
    .btn-confirm[data-danger="true"] {
      background: var(--zn-danger, #dc2626);
    }
    .btn-cancel:hover, .btn-confirm:hover { opacity: 0.9; }
    @keyframes fade-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }
  `]
})
export class ConfirmModal {
  confirmService = inject(ConfirmService);
}
