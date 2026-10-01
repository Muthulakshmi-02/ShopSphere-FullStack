import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from './toast-service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-stack" role="status" aria-live="polite">
      <div
        class="toast"
        *ngFor="let t of toastService.toasts()"
        [attr.data-type]="t.type">
        <span class="toast-icon">
          <ng-container [ngSwitch]="t.type">
            <span *ngSwitchCase="'success'">✓</span>
            <span *ngSwitchCase="'error'">✕</span>
            <span *ngSwitchDefault>ℹ</span>
          </ng-container>
        </span>
        <span class="toast-message">{{ t.message }}</span>
        <button class="toast-close" (click)="toastService.dismiss(t.id)" aria-label="Dismiss">×</button>
      </div>
    </div>
  `,
  styles: [`
    .toast-stack {
      position: fixed;
      top: var(--zn-space-5, 24px);
      right: var(--zn-space-5, 24px);
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: var(--zn-space-3, 12px);
      max-width: 360px;
    }
    .toast {
      display: flex;
      align-items: flex-start;
      gap: var(--zn-space-3, 12px);
      background: var(--zn-surface, #fff);
      border-left: 4px solid var(--zn-info, #0284c7);
      border-radius: var(--zn-radius-md, 10px);
      box-shadow: var(--zn-shadow-lg, 0 12px 32px rgba(15,23,42,0.12));
      padding: var(--zn-space-4, 16px);
      font-size: 0.9rem;
      color: var(--zn-text-main, #1e293b);
      animation: toast-in 200ms ease;
    }
    .toast[data-type="success"] { border-left-color: var(--zn-success, #16a34a); }
    .toast[data-type="error"] { border-left-color: var(--zn-danger, #dc2626); }
    .toast-icon {
      font-weight: 700;
      flex-shrink: 0;
    }
    .toast[data-type="success"] .toast-icon { color: var(--zn-success, #16a34a); }
    .toast[data-type="error"] .toast-icon { color: var(--zn-danger, #dc2626); }
    .toast[data-type="info"] .toast-icon { color: var(--zn-info, #0284c7); }
    .toast-message { flex: 1; line-height: 1.4; }
    .toast-close {
      background: none;
      border: none;
      font-size: 1.1rem;
      line-height: 1;
      cursor: pointer;
      color: var(--zn-text-muted, #64748b);
      padding: 0;
    }
    @keyframes toast-in {
      from { opacity: 0; transform: translateX(16px); }
      to { opacity: 1; transform: translateX(0); }
    }
  `]
})
export class ToastContainer {
  toastService = inject(ToastService);
}
