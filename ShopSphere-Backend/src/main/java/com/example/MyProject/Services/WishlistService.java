package com.example.MyProject.Services;

import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.User;
import com.example.MyProject.Models.WishlistItem;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.Repository.UserRepository;
import com.example.MyProject.Repository.WishlistRepository;
import com.example.MyProject.User.Dto.ApiResponse;
import com.example.MyProject.Wishlist.dto.WishlistItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    // BUG FIX: this method maps each WishlistItem to a full ProductResponse
    // via productService.toResponse(), which touches lazy relationships
    // (item.getProduct(), then product.getCategory() inside that mapping).
    // Without @Transactional here, and with open-in-view=false (see
    // application.properties), the Hibernate session closes as soon as the
    // repository call returns - so accessing those lazy fields afterward
    // threw a LazyInitializationException, a 500 error, which is exactly
    // why "add to wishlist" (no lazy access) worked but loading the full
    // wishlist page didn't.
    @Transactional(readOnly = true)
    public ApiResponse<List<WishlistItemResponse>> getMyWishlist() {
        User user = getAuthenticatedUser();
        List<WishlistItemResponse> items = wishlistRepository
                .findByUser_UserIdOrderByAddedAtDesc(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<WishlistItemResponse>>builder()
                .success(true)
                .data(items)
                .build();
    }

    /**
     * Just the product IDs currently wishlisted by this user - used by the
     * frontend to light up the heart icon on product cards without having
     * to fetch the full wishlist on every page.
     */
    @Transactional(readOnly = true)
    public ApiResponse<Set<Long>> getMyWishlistProductIds() {
        User user = getAuthenticatedUser();
        Set<Long> ids = wishlistRepository.findByUser_UserIdOrderByAddedAtDesc(user.getUserId())
                .stream()
                .map(item -> item.getProduct().getProductId())
                .collect(Collectors.toSet());

        return ApiResponse.<Set<Long>>builder().success(true).data(ids).build();
    }

    @Transactional
    public ApiResponse<Boolean> toggleWishlist(Long productId) {
        User user = getAuthenticatedUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean alreadyWishlisted = wishlistRepository
                .existsByUser_UserIdAndProduct_ProductId(user.getUserId(), productId);

        if (alreadyWishlisted) {
            wishlistRepository.deleteByUser_UserIdAndProduct_ProductId(user.getUserId(), productId);
            return ApiResponse.<Boolean>builder()
                    .success(true)
                    .message("Removed from wishlist")
                    .data(false)
                    .build();
        }

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();
        wishlistRepository.save(item);

        return ApiResponse.<Boolean>builder()
                .success(true)
                .message("Added to wishlist")
                .data(true)
                .build();
    }

    private WishlistItemResponse mapToResponse(WishlistItem item) {
        return WishlistItemResponse.builder()
                .wishlistItemId(item.getWishlistItemId())
                .product(productService.toResponse(item.getProduct()))
                .addedAt(item.getAddedAt())
                .build();
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        String email = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
