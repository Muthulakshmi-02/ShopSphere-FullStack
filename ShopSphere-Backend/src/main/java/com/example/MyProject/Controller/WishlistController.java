package com.example.MyProject.Controller;

import com.example.MyProject.Services.WishlistService;
import com.example.MyProject.User.Dto.ApiResponse;
import com.example.MyProject.Wishlist.dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

// @CrossOrigin removed: CORS is configured once in SecurityConfig.
// Login is already required for every /api/wishlist/** call by anyRequest().authenticated().
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
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

    // Toggles membership: adds if absent, removes if present.
    @PostMapping("/{productId}/toggle")
    public ResponseEntity<ApiResponse<Boolean>> toggle(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.toggleWishlist(productId));
    }
}