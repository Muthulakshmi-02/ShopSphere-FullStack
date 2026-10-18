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
      // If a dialog is already open, answer it with "no" first. Before, the new dialog simply
      // replaced it and the code that was awaiting the first answer waited forever.
      this.active()?.resolve(false);
      this.active.set({ ...request, resolve });
    });
  }

  resolve(confirmed: boolean) {
    const current = this.active();
    this.active.set(null);          // close first, so a double-click cannot answer twice
    current?.resolve(confirmed);
  }
}