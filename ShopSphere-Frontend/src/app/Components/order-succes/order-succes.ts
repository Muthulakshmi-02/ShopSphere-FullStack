import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
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
  protected readonly productImageUrl = productImageUrl;
  orderData?: OrderResponse;

  ngOnInit() {
    // Retrieve the order data passed from the Checkout Page
    const navigation = this.router.getCurrentNavigation();
    this.orderData = window.history.state?.order;

    // If a user tries to access this page directly without an order, redirect them
    if (!this.orderData) {
      this.router.navigate(['/']);
    }
  }
}