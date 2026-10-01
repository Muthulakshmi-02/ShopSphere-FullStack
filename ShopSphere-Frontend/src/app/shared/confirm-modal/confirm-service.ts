import { Injectable, signal } from '@angular/core';

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

interface ActiveConfirm extends ConfirmRequest {
  resolve: (confirmed: boolean) => void;
}

/**
 * Promise-based confirmation dialog, replacing raw window.confirm() so
 * destructive actions (delete product, cancel order) get a real, styled
 * confirmation instead of a browser-native popup.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  active = signal<ActiveConfirm | null>(null);

  ask(request: ConfirmRequest): Promise<boolean> {
    return new Promise(resolve => {
      this.active.set({ ...request, resolve });
    });
  }

  resolve(confirmed: boolean) {
    this.active()?.resolve(confirmed);
    this.active.set(null);
  }
}
