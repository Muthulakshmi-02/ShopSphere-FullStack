export interface OrderRequest {
  shippingAddress: string;
  city: string;
  state: string;
  zipCode: string;
  phoneNumber: string;
  paymentMethod: string;
  gatewayToken?: string;
  promoCode?: string;
  productId?: number;
  quantity?: number;
  variantId?: number;
}

export interface OrderItemResponse {
  productId: number;
  productName: string;
  quantity: number;
  priceAtPurchase: number;
  imageUrl: string;
  variantLabel?: string | null;
}

export interface OrderResponse {
  orderId: number;
  orderDate: string;
  totalAmount: number;
  orderStatus: string;
  paymentStatus: string;
  paymentMethod?: string;
  promoCode?: string | null;
  discountAmount?: number | null;
  shippingAddress: string;
  items: OrderItemResponse[];
}
// PaymentRequest and RazorpayOrderResponse stay as they are.
export interface PaymentRequest {
  orderId: number;
  transactionId: string;
  paymentMethod: string;
  paymentGateway: string;
  status: string;
  // Populated from Razorpay's checkout callback - the backend verifies
  // these cryptographically, never trusts them at face value.
  razorpayOrderId?: string;
  razorpayPaymentId?: string;
  razorpaySignature?: string;
}

export interface RazorpayOrderResponse {
  razorpayOrderId: string;
  amountInPaise: number;
  currency: string;
  keyId: string;
  orderReceipt: string;
}