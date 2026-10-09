package com.example.MyProject.Services;

import com.example.MyProject.Exception.CartBusinessException;
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

    private static final int MAX_WISHLIST_ITEMS = 200;

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    // @Transactional is required: mapping to ProductResponse reads lazy relations
    // and open-in-view is off (see application.properties).
    @Transactional(readOnly = true)
    public ApiResponse<List<WishlistItemResponse>> getMyWishlist() {
        User user = getAuthenticatedUser();
        List<WishlistItemResponse> items = wishlistRepository
                .findAllWithProductByUserId(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.<List<WishlistItemResponse>>builder()
                .success(true)
                .data(items)
                .build();
    }

    /** Just the product IDs wishlisted by this user (lights up hearts on product cards). */
    @Transactional(readOnly = true)
    public ApiResponse<Set<Long>> getMyWishlistProductIds() {
        User user = getAuthenticatedUser();
        Set<Long> ids = wishlistRepository.findProductIdsByUserId(user.getUserId());
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

        // Without a cap, a script could create unlimited rows for one account.
        if (wishlistRepository.countByUser_UserId(user.getUserId()) >= MAX_WISHLIST_ITEMS) {
            throw new CartBusinessException(
                    "Your wishlist is full (" + MAX_WISHLIST_ITEMS + " items). Remove something first.");
        }

        wishlistRepository.save(WishlistItem.builder()
                .user(user)
                .product(product)
                .build());

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