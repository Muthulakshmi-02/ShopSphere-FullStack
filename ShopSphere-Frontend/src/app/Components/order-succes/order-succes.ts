import { Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, CurrencyPipe, isPlatformBrowser } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { OrderResponse } from '../../Models/Order.model';
import { productImageUrl } from '../../shared/api.config';
import { OrderTimeline } from '../../shared/order-timeline/order-timeline';

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterModule, OrderTimeline],
  templateUrl: './order-succes.html',
  styleUrls: ['./order-succes.css']
})
export class OrderSuccess implements OnInit {
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);
  protected readonly productImageUrl = productImageUrl;
  orderData?: OrderResponse;

  constructor() {
    // The order is handed over by the checkout page via router state. It has to be read here, while
    // the navigation is still in progress; inside ngOnInit getCurrentNavigation() is already null.
    this.orderData = this.router.getCurrentNavigation()?.extras.state?.['order'];
  }

  ngOnInit() {
    // window / history do not exist during server-side rendering. The old code used window.history
    // directly, which would have crashed the server render of this page.
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    if (!this.orderData) {
      this.orderData = history.state?.order;
    }

    // Opened directly or refreshed (the router state is gone): the order is still in the history.
    if (!this.orderData) {
      this.router.navigate(['/my-orders']);
    }
  }

  get isCod(): boolean {
    return this.orderData?.paymentMethod === 'COD';
  }
}