package com.example.MyProject.Cart.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartItemRequest {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private Double price;

    // Null for a simple (non-variant) product, same as every cart item
    // worked before variants existed.
    private Long variantId;
    // Human-readable snapshot, e.g. "Size: 9, Color: Black", for display
    // in the cart without the frontend having to reconstruct it.
    private String variantLabel;
}