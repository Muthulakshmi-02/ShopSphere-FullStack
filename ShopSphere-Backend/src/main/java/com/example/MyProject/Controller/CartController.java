//package com.example.MyProject.Controller;
//
//import com.example.MyProject.Cart.dto.CartRequest;
//import com.example.MyProject.Services.CartService;
//import com.example.MyProject.User.Dto.ApiResponse;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.MessageSource;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Locale;
//
//
//@CrossOrigin(origins = "http://localhost:4500", allowCredentials = "true")
//@RestController
//@RequestMapping("/api/cart")
//@RequiredArgsConstructor
//@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
//public class CartController {
//
//    private final CartService cartService;
//    private final MessageSource messageSource;
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<CartRequest>> getCart() {
//        CartRequest data = cartService.getCart();
//        return ResponseEntity.ok(new ApiResponse<>(true, data, "Cart fetched"));
//    }
//
//    @PostMapping("/add/{productId}/{quantity}")
//    public ResponseEntity<ApiResponse<CartRequest>> addItem(
//            @PathVariable Long productId, @PathVariable Integer quantity,
//            @RequestParam(required = false) Long variantId) {
//        CartRequest data = cartService.addProductToCart(productId, quantity, variantId);
//
//        return ResponseEntity.ok(new ApiResponse<>(true, data, getMessage("cart.item.added")));
//    }
//
//    @PutMapping("/update/{productId}/{quantity}")
//    public ResponseEntity<ApiResponse<CartRequest>> updateItem(
//            @PathVariable Long productId, @PathVariable Integer quantity,
//            @RequestParam(required = false) Long variantId) {
//        CartRequest data = cartService.updateQuantity(productId, quantity, variantId);
//
//        return ResponseEntity.ok(new ApiResponse<>(true, data, getMessage("cart.item.updated")));
//    }
//
//    @DeleteMapping("/remove/{productId}")
//    public ResponseEntity<ApiResponse<CartRequest>> removeItem(
//            @PathVariable Long productId,
//            @RequestParam(required = false) Long variantId) {
//        CartRequest data = cartService.removeItem(productId, variantId);
//
//        return ResponseEntity.ok(new ApiResponse<>(true, data, getMessage("cart.item.removed")));
//    }
//
//    @Transactional
//    @DeleteMapping("/clear")
//    public ResponseEntity<ApiResponse<Void>> clearCart() {
//
//        cartService.clearCart();
//
//        return ResponseEntity.ok(new ApiResponse<>(true, null, getMessage("cart.cleared")));
//    }
//
//    private String getMessage(String code) {
//        return messageSource.getMessage(code, null, Locale.getDefault());
//    }
//}

package com.example.MyProject.Controller;

import com.example.MyProject.Cart.dto.CartRequest;
import com.example.MyProject.Services.CartService;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Removed: @CrossOrigin (CORS lives in SecurityConfig) and the jakarta @Transactional on
// clearCart (the service already owns the transaction).
// @PreAuthorize is now USER only, matching SecurityConfig: "/api/cart/**" was already USER-only
// there, so the old "or hasRole('ADMIN')" here never actually applied.
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CartController {

    private final CartService cartService;
    private final MessageSource messageSource;

    @GetMapping
    public ResponseEntity<ApiResponse<CartRequest>> getCart() {
        CartRequest data = cartService.getCart();
        return ResponseEntity.ok(new ApiResponse<>(true, data, "Cart fetched"));
    }

    @PostMapping("/add/{productId}/{quantity}")
    public ResponseEntity<ApiResponse<CartRequest>> addItem(
            @PathVariable Long productId, @PathVariable Integer quantity,
            @RequestParam(required = false) Long variantId) {
        CartRequest data = cartService.addProductToCart(productId, quantity, variantId);
        return ResponseEntity.ok(new ApiResponse<>(true, data, getMessage("cart.item.added")));
    }

    @PutMapping("/update/{productId}/{quantity}")
    public ResponseEntity<ApiResponse<CartRequest>> updateItem(
            @PathVariable Long productId, @PathVariable Integer quantity,
            @RequestParam(required = false) Long variantId) {
        CartRequest data = cartService.updateQuantity(productId, quantity, variantId);
        return ResponseEntity.ok(new ApiResponse<>(true, data, getMessage("cart.item.updated")));
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<CartRequest>> removeItem(
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId) {
        CartRequest data = cartService.removeItem(productId, variantId);
        return ResponseEntity.ok(new ApiResponse<>(true, data, getMessage("cart.item.removed")));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(new ApiResponse<>(true, null, getMessage("cart.cleared")));
    }

    /** Preview of a promo code for the logged-in user (the real discount is applied at checkout). */
    @GetMapping("/validate-promo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validatePromoCode(@RequestParam String code) {
        Map<String, Object> promoDetails = cartService.validatePromoCode(code);
        return ResponseEntity.ok(new ApiResponse<>(true, promoDetails, "Promo code validated"));
    }

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }
}