/**
 * Matches: CartItemRequest.java & CartItemResponse.java
 * These fields represent the items currently inside a user's cart.
 */
export interface CartItemRequest {
  cartItemId: number;    // Primary key of the cart item record
  productId: number;     // Reference to the Product ID
  productName: string;   // Display name
  imageUrl: string;      // Product image URL
  quantity: number;      // Current quantity in cart
  price: number;         // Price at the time of adding to cart
  total?: number;        // Optional: quantity * price (calculated field)
  variantId?: number | null;    // Null for a simple (non-variant) product
  variantLabel?: string | null; // e.g. "Size: 9, Color: Black"
}

/**
 * Matches: CartRequest.java
 * This is the root object returned by your CartController.
 */
export interface CartRequest {
  cartId: number;              // The unique ID of the user's cart
  items: CartItemRequest[];    // List of items in the cart
  totalAmount: number;         // The final total calculated by the backend
}

/**
 * Helper interface for the ApiResponse wrapper 
 * (Matches your ApiResponse.java structure)
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}