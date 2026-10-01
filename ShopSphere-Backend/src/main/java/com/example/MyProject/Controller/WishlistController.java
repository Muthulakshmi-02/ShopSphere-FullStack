package com.example.MyProject.Controller;

import com.example.MyProject.Services.WishlistService;
import com.example.MyProject.User.Dto.ApiResponse;
import com.example.MyProject.Wishlist.dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4500", allowCredentials = "true")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getMyWishlist() {
        return ResponseEntity.ok(wishlistService.getMyWishlist());
    }

    @GetMapping("/product-ids")
    public ResponseEntity<ApiResponse<Set<Long>>> getMyWishlistProductIds() {
        return ResponseEntity.ok(wishlistService.getMyWishlistProductIds());
    }

    /**
     * Toggles wishlist membership for a product - adds it if not present,
     * removes it if already there. Keeps the frontend's "heart" button
     * logic to a single call instead of needing to know current state first.
     */
    @PostMapping("/{productId}/toggle")
    public ResponseEntity<ApiResponse<Boolean>> toggle(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.toggleWishlist(productId));
    }
}
