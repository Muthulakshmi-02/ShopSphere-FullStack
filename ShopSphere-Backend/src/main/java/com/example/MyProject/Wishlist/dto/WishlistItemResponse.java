package com.example.MyProject.Wishlist.dto;

import com.example.MyProject.Product.Dto.ProductResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponse {
    private Long wishlistItemId;
    private ProductResponse product;
    private LocalDateTime addedAt;
}
