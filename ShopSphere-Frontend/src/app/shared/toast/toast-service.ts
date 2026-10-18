import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

/**
 * App-wide toast notifications, replacing raw alert() popups so errors and
 * confirmations feel like part of the product instead of a browser dialog.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private static readonly MAX_VISIBLE = 4;

  private nextId = 1;
  toasts = signal<Toast[]>([]);

  show(message: string, type: ToastType = 'info', durationMs = 4000) {
    // The same message is already on screen: don't stack a duplicate (e.g. a failing request that
    // retries, or repeated clicks).
    if (this.toasts().some(t => t.message === message && t.type === type)) return;

    const id = this.nextId++;
    // At most 4 at once; the oldest is dropped, so a burst of errors cannot cover the page.
    this.toasts.update(list => [...list, { id, message, type }].slice(-ToastService.MAX_VISIBLE));

    // Long messages (the server now explains why an action was refused) get more reading time.
    const duration = Math.max(durationMs, Math.min(message.length * 60, 10000));
    setTimeout(() => this.dismiss(id), duration);
  }

  success(message: string) { this.show(message, 'success'); }
  error(message: string) { this.show(message, 'error', 5000); }
  info(message: string) { this.show(message, 'info'); }

  dismiss(id: number) {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}