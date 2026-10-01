export enum ProductStatus {
  IN_STOCK = 'IN_STOCK',
  OUT_OF_STOCK = 'OUT_OF_STOCK'
}

/**
 * Matches Backend: ProductVariantResponse.java / ProductVariantRequest.java
 * A size/color option under a product, each with its own stock.
 */
export interface ProductVariantResponse {
  variantId: number;
  size: string | null;
  color: string | null;
  stock: number;
  priceOverride: number | null;
  sku: string | null;
}

export interface ProductVariantRequest {
  variantId?: number | null; // present = editing existing, null/absent = new
  size: string | null;
  color: string | null;
  stock: number;
  priceOverride: number | null;
  sku: string | null;
}

/**
 * Matches your Backend: ProductResponse.java
 * Used for displaying data in the Dashboard and Grids.
 */
export interface ProductResponse {
  productId: number;     // private Long productId
  name: string;          // private String name
  description: string;   // private String description
  price: number;         // private Double price
  stock: number;         // private Integer stock
  imageUrl: string;      // private String imageUrl
  status: ProductStatus; // private ProductStatus status (Enum)
  rating?: number;       // private Double rating (0-5, optional)
  reviewCount?: number;  // private Integer reviewCount (optional)
  discountPercentage?: number; // private Integer discountPercentage (0-90, optional)
  categoryId: number;    // private Long categoryId
  categoryName: string;  // private String categoryName
  variants: ProductVariantResponse[]; // empty array = simple product, no variants
  isNew?: boolean;
  inCart?: boolean;
  outOfStock?: boolean;
}

/**
 * Matches your Backend: ProductRequest.java
 * Used when the Admin creates or updates a product.
 */
export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stock: number;
  imageUrl: string;
  status: ProductStatus;
  rating?: number | null;
  reviewCount?: number | null;
  discountPercentage?: number | null;
  categoryId: number;
  variants: ProductVariantRequest[];
}

// Shortcut alias for cleaner code in your components
export type Product = ProductResponse;