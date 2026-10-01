//package com.example.MyProject.Services;
//import com.example.MyProject.Repository.CouponRepository;
//import com.example.MyProject.Cart.dto.CartRequest;
//import com.example.MyProject.Cart.dto.CartItemRequest;
//import com.example.MyProject.Exception.CartBusinessException;
//import com.example.MyProject.Exception.ResourceNotFoundException;
//import com.example.MyProject.Models.Cart;
//import com.example.MyProject.Models.CartItem;
//import com.example.MyProject.Models.Product;
//import com.example.MyProject.Models.ProductVariant;
//import com.example.MyProject.Models.User;
//import com.example.MyProject.Repository.CartRepository;
//import com.example.MyProject.Repository.ProductRepository;
//import com.example.MyProject.Repository.ProductVariantRepository;
//import com.example.MyProject.Repository.UserRepository;
//import com.stripe.model.Coupon;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.MessageSource;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class CartService {
//
//    private final UserRepository userRepository;
//    private final CartRepository cartRepository;
//    private final ProductRepository productRepository;
//    private final ProductVariantRepository productVariantRepository;
//    private final MessageSource messageSource;
//    private final CouponRepository couponRepository;
//    private final UserCouponRepository userCouponRepository;
//    @Transactional
//    public CartRequest addProductToCart(Long productId, Integer quantity, Long variantId) {
//        User user = getAuthenticatedUser();
//
//        // 1. Get or Create Cart
//        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
//                .orElseGet(() -> createNewCart(user));
//
//        // Ensure the items list is initialized
//        if (cart.getItems() == null) {
//            cart.setItems(new ArrayList<>());
//        }
//
//        // 2. Check if this exact (product, variant) combination already exists
//        // in the cart - a product with variants can have multiple separate
//        // cart entries (e.g. Size 9 AND Size 10 of the same product), so this
//        // now matches on both fields, not just productId.
//        boolean exists = cart.getItems().stream()
//                .anyMatch(item -> item.getProduct().getProductId().equals(productId)
//                        && Objects.equals(variantIdOf(item), variantId));
//
//        if (exists) {
//            throw new CartBusinessException(getMessage("cart.item.exists"));
//        }
//
//        // 3. Validation: Check if product exists
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new ResourceNotFoundException(getMessage("product.not.found", productId)));
//
//        ProductVariant variant = null;
//        double unitPrice;
//
//        if (variantId != null) {
//            variant = productVariantRepository.findByVariantIdAndProduct_ProductId(variantId, productId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found for this product"));
//
//            if (variant.getStock() < quantity) {
//                throw new CartBusinessException(getMessage("product.out.of.stock", product.getName()));
//            }
//            unitPrice = variant.getPriceOverride() != null ? variant.getPriceOverride() : product.getPrice();
//        } else {
//            if (!product.getVariants().isEmpty()) {
//                // This product is sold through variants - selecting one is
//                // required, adding the "bare" product directly isn't allowed
//                // once it has variants (mirrors how checkout/stock is tracked).
//                throw new CartBusinessException("Please select a size/color option for this product.");
//            }
//            if (product.getStock() < quantity) {
//                throw new CartBusinessException(getMessage("product.out.of.stock", product.getName()));
//            }
//            unitPrice = product.getPrice();
//        }
//
//        // 4. Create and Link new CartItem
//        CartItem newItem = new CartItem();
//        newItem.setProduct(product);
//        newItem.setVariant(variant);
//        newItem.setQuantity(quantity);
//        newItem.setPrice(unitPrice);
//        newItem.setCart(cart); // Critical: Link item to cart for database foreign key
//
//        cart.getItems().add(newItem);
//
//        // 5. Update the total amount immediately
//        double newTotal = cart.getItems().stream()
//                .mapToDouble(i -> i.getPrice() * i.getQuantity())
//                .sum();
//        cart.setTotalAmount(Math.round(newTotal * 100.0) / 100.0);
//
//        return saveAndMap(cart);
//    }
//
//    @Transactional(readOnly = true)
//    public CartRequest getCart() {
//        return mapToRequest(getCartEntity());
//    }
//
//    @Transactional
//    public CartRequest updateQuantity(Long productId, Integer quantity, Long variantId) {
//        Cart cart = getCartEntity();
//        CartItem item = findItemInCart(cart, productId, variantId);
//
//        int availableStock = item.getVariant() != null
//                ? item.getVariant().getStock()
//                : item.getProduct().getStock();
//
//        if (availableStock < quantity) {
//            throw new CartBusinessException(getMessage("product.out.of.stock", item.getProduct().getName()));
//        }
//
//        item.setQuantity(quantity);
//        return saveAndMap(cart);
//    }
//
//    @Transactional
//    public CartRequest removeItem(Long productId, Long variantId) {
//        Cart cart = getCartEntity();
//        CartItem item = findItemInCart(cart, productId, variantId);
//
//        cart.getItems().remove(item);
//        return saveAndMap(cart);
//    }
//
//    @Transactional
//    public void clearCart() {
//        Cart cart = getCartEntity();
//        if (cart.getItems() != null) {
//            cart.getItems().clear();
//        }
//        cart.setTotalAmount(0.0);
//        cartRepository.save(cart);
//    }
//
//    // --- Private Helper Methods ---
//
//    private Long variantIdOf(CartItem item) {
//        return item.getVariant() != null ? item.getVariant().getVariantId() : null;
//    }
//
//    private User getAuthenticatedUser() {
//        var authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            throw new RuntimeException("No authenticated user found");
//        }
//
//        Object principal = authentication.getPrincipal();
//        String email;
//
//        if (principal instanceof UserDetails) {
//            email = ((UserDetails) principal).getUsername();
//        } else {
//            email = principal.toString();
//        }
//
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found in database with email: " + email));
//    }
//
//    private Cart getCartEntity() {
//        User user = getAuthenticatedUser();
//        return cartRepository.findByUser_UserId(user.getUserId())
//                .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.not.found")));
//    }
//
//    private CartItem findItemInCart(Cart cart, Long productId, Long variantId) {
//        if (cart.getItems() == null) {
//            throw new ResourceNotFoundException(getMessage("cart.item.not.found"));
//        }
//        return cart.getItems().stream()
//                .filter(i -> i.getProduct().getProductId().equals(productId)
//                        && Objects.equals(variantIdOf(i), variantId))
//                .findFirst()
//                .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.item.not.found")));
//    }
//
//    private CartRequest saveAndMap(Cart cart) {
//        // Recalculate total before final save
//        double total = cart.getItems().stream()
//                .mapToDouble(i -> i.getPrice() * i.getQuantity())
//                .sum();
//        cart.setTotalAmount(Math.round(total * 100.0) / 100.0);
//
//        // Use saveAndFlush to ensure IDs are generated for the mapping step
//        Cart savedCart = cartRepository.saveAndFlush(cart);
//        return mapToRequest(savedCart);
//    }
//
//    private CartRequest mapToRequest(Cart cart) {
//        CartRequest request = new CartRequest();
//        request.setCartId(cart.getCartId());
//        request.setTotalAmount(cart.getTotalAmount());
//
//        if (cart.getItems() != null) {
//            request.setItems(cart.getItems().stream().map(item -> {
//                CartItemRequest iReq = new CartItemRequest();
//                iReq.setCartItemId(item.getCartItemId());
//                iReq.setProductId(item.getProduct().getProductId());
//                iReq.setProductName(item.getProduct().getName());
//                iReq.setImageUrl(item.getProduct().getImageUrl());
//                iReq.setQuantity(item.getQuantity());
//                iReq.setPrice(item.getPrice());
//                if (item.getVariant() != null) {
//                    iReq.setVariantId(item.getVariant().getVariantId());
//                    iReq.setVariantLabel(buildVariantLabel(item.getVariant()));
//                }
//                return iReq;
//            }).collect(Collectors.toList()));
//        } else {
//            request.setItems(new ArrayList<>());
//        }
//
//        return request;
//    }
//
//    private String buildVariantLabel(ProductVariant variant) {
//        StringBuilder sb = new StringBuilder();
//        if (variant.getSize() != null && !variant.getSize().isBlank()) {
//            sb.append("Size: ").append(variant.getSize());
//        }
//        if (variant.getColor() != null && !variant.getColor().isBlank()) {
//            if (sb.length() > 0) sb.append(", ");
//            sb.append("Color: ").append(variant.getColor());
//        }
//        return sb.toString();
//    }
//
//    private Cart createNewCart(User user) {
//        Cart cart = new Cart();
//        cart.setUser(user);
//        cart.setTotalAmount(0.0);
//        cart.setCreatedAt(LocalDateTime.now());
//        cart.setItems(new ArrayList<>());
//        return cartRepository.save(cart);
//    }
//
//    private String getMessage(String code, Object... args) {
//        try {
//            return messageSource.getMessage(code, args, Locale.getDefault());
//        } catch (Exception e) {
//            return "Error: " + code;
//        }
//    }
//    public Map<String, Object> validatePromoCode(String code) {
//        String cleanCode = code.trim().toUpperCase();
//
//        // Get current authenticated user's email/username
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String userEmail = authentication.getName();
//
//        // 1. Check if user already used this promo code
//        // (Adjust method name based on your user identification field)
//        boolean alreadyUsed = userCouponRepository.existsByUserEmailAndCouponCode(userEmail, cleanCode);
//        if (alreadyUsed) {
//            return Map.of(
//                    "valid", false,
//                    "used", true,
//                    "message", "You have already used this promo code on a previous order."
//            );
//        }
//
//        // 2. Fetch coupon from database
//        Optional<Coupon> couponOpt = couponRepository.findByCodeAndActiveTrue(cleanCode);
//        if (couponOpt.isPresent()) {
//            Coupon coupon = couponOpt.get();
//            return Map.of(
//                    "valid", true,
//                    "used", false,
//                    "type", coupon.getType(), // Make sure Coupon entity has getType() or getDiscountType()
//                    "value", coupon.getValue() // Make sure Coupon entity has getValue() or getAmount()
//            );
//        }
//
//        return Map.of(
//                "valid", false,
//                "used", false,
//                "message", "Invalid or expired promo code."
//        );
//    }
//}
package com.example.MyProject.Services;

import com.example.MyProject.Cart.dto.CartItemRequest;
import com.example.MyProject.Cart.dto.CartRequest;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Models.Cart;
import com.example.MyProject.Models.CartItem;
import com.example.MyProject.Models.Coupon;
import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.ProductVariant;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.CartRepository;
import com.example.MyProject.Repository.CouponRepository;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.Repository.ProductVariantRepository;
import com.example.MyProject.Repository.UserCouponRepository;
import com.example.MyProject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final MessageSource messageSource;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PricingService pricing;

    // ------------------------------------------------------------------
    // Cart operations
    // ------------------------------------------------------------------

    /**
     * Returns the cart (creating an empty one for first-time users instead of a 404)
     * and re-prices it, so a price/discount change never leaves a stale cart.
     */
    @Transactional
    public CartRequest getCart() {
        User user = getAuthenticatedUser();
        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> createNewCart(user));
        return saveAndMap(cart);
    }

    @Transactional
    public CartRequest addProductToCart(Long productId, Integer quantity, Long variantId) {
        requireValidQuantity(quantity);
        User user = getAuthenticatedUser();

        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> createNewCart(user));

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        boolean exists = cart.getItems().stream()
                .anyMatch(item -> item.getProduct().getProductId().equals(productId)
                        && Objects.equals(variantIdOf(item), variantId));
        if (exists) {
            throw new CartBusinessException(getMessage("cart.item.exists"));
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(getMessage("product.not.found", productId)));

        ProductVariant variant = null;

        if (variantId != null) {
            variant = productVariantRepository
                    .findByVariantIdAndProduct_ProductId(variantId, productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found for this product"));

            if (variant.getStock() < quantity) {
                throw new CartBusinessException(getMessage("product.out.of.stock", product.getName()));
            }
        } else {
            if (!product.getVariants().isEmpty()) {
                throw new CartBusinessException("Please select a size/color option for this product.");
            }
            if (product.getStock() < quantity) {
                throw new CartBusinessException(getMessage("product.out.of.stock", product.getName()));
            }
        }

        CartItem newItem = new CartItem();
        newItem.setProduct(product);
        newItem.setVariant(variant);
        newItem.setQuantity(quantity);
        newItem.setPrice(pricing.unitPrice(product, variant));   // shared formula
        newItem.setCart(cart);
        cart.getItems().add(newItem);

        return saveAndMap(cart);   // recalculates the total
    }

    @Transactional
    public CartRequest updateQuantity(Long productId, Integer quantity, Long variantId) {
        requireValidQuantity(quantity);
        Cart cart = getCartEntity();
        CartItem item = findItemInCart(cart, productId, variantId);

        int availableStock = item.getVariant() != null
                ? item.getVariant().getStock()
                : item.getProduct().getStock();

        if (availableStock < quantity) {
            throw new CartBusinessException(getMessage("product.out.of.stock", item.getProduct().getName()));
        }

        item.setQuantity(quantity);
        return saveAndMap(cart);
    }

    @Transactional
    public CartRequest removeItem(Long productId, Long variantId) {
        Cart cart = getCartEntity();
        CartItem item = findItemInCart(cart, productId, variantId);

        cart.getItems().remove(item);
        return saveAndMap(cart);
    }

    @Transactional
    public void clearCart() {
        Cart cart = getCartEntity();
        if (cart.getItems() != null) {
            cart.getItems().clear();
        }
        cart.setTotalAmount(0.0);
        cartRepository.save(cart);
    }

    // ------------------------------------------------------------------
    // Promo preview (the real discount is applied in PromoService at checkout)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> validatePromoCode(String code) {
        String cleanCode = code.trim().toUpperCase();
        User currentUser = getAuthenticatedUser();

        boolean alreadyUsed = userCouponRepository.existsByUserEmailAndCouponCode(currentUser.getEmail(), cleanCode);
        if (alreadyUsed) {
            return Map.of(
                    "valid", false,
                    "used", true,
                    "message", "You have already used this promo code on a previous order."
            );
        }

        Optional<Coupon> couponOpt = couponRepository.findByCodeAndActiveTrue(cleanCode);
        if (couponOpt.isPresent()) {
            Coupon coupon = couponOpt.get();
            return Map.of(
                    "valid", true,
                    "used", false,
                    "type", coupon.getType(),
                    "value", coupon.getValue()
            );
        }

        return Map.of(
                "valid", false,
                "used", false,
                "message", "Invalid or expired promo code."
        );
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void requireValidQuantity(Integer q) {
        if (q == null || q < 1 || q > 99) {
            throw new CartBusinessException("Quantity must be between 1 and 99.");
        }
    }

    private void repriceItems(Cart cart) {
        if (cart.getItems() == null) return;
        for (CartItem i : cart.getItems()) {
            i.setPrice(pricing.unitPrice(i.getProduct(), i.getVariant()));
        }
    }

    private CartRequest saveAndMap(Cart cart) {
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }
        repriceItems(cart);
        double total = cart.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
        cart.setTotalAmount(pricing.round2(total));
        return mapToRequest(cartRepository.saveAndFlush(cart));
    }

    private Long variantIdOf(CartItem item) {
        return item.getVariant() != null ? item.getVariant().getVariantId() : null;
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
                .orElseThrow(() -> new RuntimeException("User not found in database with email: " + email));
    }

    private Cart getCartEntity() {
        User user = getAuthenticatedUser();
        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.not.found")));
    }

    private CartItem findItemInCart(Cart cart, Long productId, Long variantId) {
        if (cart.getItems() == null) {
            throw new ResourceNotFoundException(getMessage("cart.item.not.found"));
        }
        return cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId)
                        && Objects.equals(variantIdOf(i), variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.item.not.found")));
    }

    private CartRequest mapToRequest(Cart cart) {
        CartRequest request = new CartRequest();
        request.setCartId(cart.getCartId());
        request.setTotalAmount(cart.getTotalAmount());

        if (cart.getItems() != null) {
            request.setItems(cart.getItems().stream().map(item -> {
                CartItemRequest iReq = new CartItemRequest();
                iReq.setCartItemId(item.getCartItemId());
                iReq.setProductId(item.getProduct().getProductId());
                iReq.setProductName(item.getProduct().getName());
                iReq.setImageUrl(item.getProduct().getImageUrl());
                iReq.setQuantity(item.getQuantity());
                iReq.setPrice(item.getPrice());
                if (item.getVariant() != null) {
                    iReq.setVariantId(item.getVariant().getVariantId());
                    iReq.setVariantLabel(buildVariantLabel(item.getVariant()));
                }
                return iReq;
            }).collect(Collectors.toList()));
        } else {
            request.setItems(new ArrayList<>());
        }

        return request;
    }

    private String buildVariantLabel(ProductVariant variant) {
        StringBuilder sb = new StringBuilder();
        if (variant.getSize() != null && !variant.getSize().isBlank()) {
            sb.append("Size: ").append(variant.getSize());
        }
        if (variant.getColor() != null && !variant.getColor().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Color: ").append(variant.getColor());
        }
        return sb.toString();
    }

    private Cart createNewCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(0.0);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setItems(new ArrayList<>());
        return cartRepository.save(cart);
    }

    private String getMessage(String code, Object... args) {
        try {
            return messageSource.getMessage(code, args, Locale.getDefault());
        } catch (Exception e) {
            return "Error: " + code;
        }
    }
}