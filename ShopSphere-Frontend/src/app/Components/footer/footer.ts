import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="site-footer">
      <div class="footer-inner">
        <span class="footer-brand">© {{ year }} Your Store</span>
        <nav class="footer-links">
          <a routerLink="/products">Shop</a>
          <a routerLink="/terms">Terms of Service</a>
          <a routerLink="/privacy">Privacy Policy</a>
        </nav>
      </div>
    </footer>
  `,
  styles: [`
    .site-footer {
      margin-top: var(--zn-space-8, 48px);
      border-top: 1px solid var(--zn-border, #e2e8f0);
      background: var(--zn-surface, #fff);
    }
    .footer-inner {
      max-width: 1200px;
      margin: 0 auto;
      padding: var(--zn-space-5, 24px) var(--zn-space-4, 16px);
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: var(--zn-space-3, 12px);
    }
    .footer-brand {
      color: var(--zn-text-muted, #64748b);
      font-size: 0.85rem;
    }
    .footer-links {
      display: flex;
      gap: var(--zn-space-5, 24px);
    }
    .footer-links a {
      color: var(--zn-text-muted, #64748b);
      text-decoration: none;
      font-size: 0.85rem;
      font-weight: 500;
    }
    .footer-links a:hover {
      color: var(--zn-primary, #6366f1);
    }
  `]
})
export class Footer {
  year = new Date().getFullYear();
}
