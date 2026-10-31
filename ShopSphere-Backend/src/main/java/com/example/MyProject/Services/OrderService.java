
// package com.example.MyProject.Services;
// import com.example.MyProject.Enum.PaymentStatus;
// import com.example.MyProject.Models.*;
// import com.example.MyProject.Order.dto.*;
// import com.example.MyProject.Enum.OrderStatus;
// import com.example.MyProject.Exception.CartBusinessException;
// import com.example.MyProject.Exception.ResourceNotFoundException;
// import com.example.MyProject.Repository.*;
// import lombok.RequiredArgsConstructor;
// import org.springframework.context.MessageSource;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.context.ApplicationEventPublisher;
// // Add import for OrderEmailEvent if it is in another package:
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Locale;
// import java.util.stream.Collectors;
// import java.util.Map;
// import java.util.Set;

// @Service
// @RequiredArgsConstructor
// public class OrderService {

//     /** Unpaid card orders hold stock; release it after this long. */
//     private static final int UNPAID_ORDER_TTL_MINUTES = 30;

//     private final OrderRepository orderRepository;
//     private final CartRepository cartRepository;
//     private final ProductRepository productRepository;
//     private final ProductVariantRepository productVariantRepository;
//     private final UserRepository userRepository;
//     private final MessageSource messageSource;
//     private final PricingService pricing;
//     private final PromoService promoService;
//     private final ApplicationEventPublisher events;

//     // constant to add inside the class
//     private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
//             OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
//             OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
//             OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
//             OrderStatus.DELIVERED, Set.of(),
//             OrderStatus.CANCELLED, Set.of());

//     @Transactional
//     public OrderResponse checkout(OrderRequest request) {
//         User user = getAuthenticatedUser();
//         boolean cod = "COD".equalsIgnoreCase(request.getPaymentMethod());

//         Order order = new Order();
//         order.setUser(user);
//         order.setOrderDate(LocalDateTime.now());
//         // COD is confirmed straight away; card orders stay PENDING until payment is
//         // verified.
//         order.setOrderStatus(cod ? OrderStatus.CONFIRMED : OrderStatus.PENDING);
//         order.setCreatedAt(LocalDateTime.now());
//         order.setPhoneNumber(request.getPhoneNumber());
//         order.setPaymentMethod(request.getPaymentMethod());
//         order.setPaymentStatus(PaymentStatus.PENDING);
//         order.setCity(request.getCity());
//         order.setState(request.getState());
//         order.setZipCode(request.getZipCode());
//         order.setShippingAddress(String.format("%s, %s, %s - %s",
//                 request.getShippingAddress(), request.getCity(), request.getState(), request.getZipCode()));

//         List<OrderItem> orderItems = new ArrayList<>();
//         Cart cart = null;

//         if (request.getProductId() != null) {
//             // PATH A: Buy Now (bypasses the cart)
//             int qty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
//             orderItems.add(buildOrderItem(order, request.getProductId(), request.getVariantId(), qty));
//         } else {
//             // PATH B: standard cart checkout
//             cart = cartRepository.findByUser_UserId(user.getUserId())
//                     .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.not.found")));

//             if (cart.getItems().isEmpty()) {
//                 throw new CartBusinessException(getMessage("order.cart.empty"));
//             }

//             // Consistent lock ordering to avoid deadlocks
//             List<CartItem> sortedItems = cart.getItems().stream()
//                     .sorted((a, b) -> {
//                         int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
//                         if (byProduct != 0)
//                             return byProduct;
//                         Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
//                         Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
//                         return Long.compare(va, vb);
//                     })
//                     .collect(Collectors.toList());

//             for (CartItem ci : sortedItems) {
//                 Long variantId = ci.getVariant() != null ? ci.getVariant().getVariantId() : null;
//                 orderItems.add(buildOrderItem(order, ci.getProduct().getProductId(), variantId, ci.getQuantity()));
//             }
//         }

//         // Prices always come from PricingService, never from the client or a stale cart
//         // row.
//         double subtotal = 0.0;
//         for (OrderItem item : orderItems) {
//             subtotal += item.getPrice() * item.getQuantity();
//         }
//         subtotal = pricing.round2(subtotal);

//         // Promo is validated and applied here, on the server.
//         PromoService.PromoResult promo = promoService.resolve(request.getPromoCode(), user.getEmail(), subtotal);
//         double discount = promo != null ? promo.discount() : 0.0;

//         order.setTotalAmount(pricing.round2(subtotal - discount));
//         order.setPromoCode(promo != null ? promo.code() : null);
//         order.setDiscountAmount(discount);

//        if (cart != null && cod) {
//             cart.getItems().clear();
//             cart.setTotalAmount(0.0);
//             cartRepository.save(cart);
//         }

//         order.setOrderItems(orderItems);
//         Order savedOrder = orderRepository.save(order);

//         // Reserve the code now; it is released again if this order is cancelled unpaid.
//         if (promo != null) {
//             promoService.record(user.getEmail(), promo.code());
//         }

//         return mapToResponse(savedOrder);
//     }

//     /**
//      * Locks and decrements stock for one line and prices it.
//      * Used by BOTH Buy Now and cart checkout so they cannot diverge.
//      */
//     private OrderItem buildOrderItem(Order order, Long productId, Long variantId, int qty) {
//         OrderItem item = new OrderItem();
//         item.setOrder(order);
//         item.setQuantity(qty);

//         if (variantId != null) {
//             ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
//                     .orElseThrow(() -> new ResourceNotFoundException(
//                             getMessage("order.product.outOfStock", "Product Variant")));

//             Product product = variant.getProduct();
//             if (!product.getProductId().equals(productId)) {
//                 throw new CartBusinessException("Selected option does not belong to this product.");
//             }
//             if (variant.getStock() < qty) {
//                 throw new CartBusinessException(getMessage("order.product.outOfStock",
//                         product.getName() + " (" + buildVariantLabel(variant) + ")"));
//             }

//             variant.setStock(variant.getStock() - qty);
//             productVariantRepository.save(variant);

//             item.setProduct(product);
//             item.setVariant(variant);
//             item.setVariantLabel(buildVariantLabel(variant));
//             item.setPrice(pricing.unitPrice(product, variant));

//             Product parent = productRepository.findByIdForUpdate(productId).orElse(product);
//             int totalVariantStock = parent.getVariants().stream()
//                     .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock() : v.getStock())
//                     .sum();
//             parent.setStock(totalVariantStock);
//             syncStockStatus(parent);
//             productRepository.save(parent);
//         } else {
//             Product product = productRepository.findByIdForUpdate(productId)
//                     .orElseThrow(
//                             () -> new ResourceNotFoundException(getMessage("order.product.outOfStock", "Product")));

//             if (!product.getVariants().isEmpty()) {
//                 throw new CartBusinessException("Please select a size/color option for this product.");
//             }
//             if (product.getStock() < qty) {
//                 throw new CartBusinessException(getMessage("order.product.outOfStock", product.getName()));
//             }

//             product.setStock(product.getStock() - qty);
//             syncStockStatus(product);
//             productRepository.save(product);

//             item.setProduct(product);
//             item.setPrice(pricing.unitPrice(product, null));
//         }
//         return item;
//     }

//     @Transactional(readOnly = true)
//     public List<OrderResponse> getUserOrderHistory() {
//         User user = getAuthenticatedUser();
//         return orderRepository.findByUserIdWithItems(user.getUserId())
//                 .stream()
//                 .map(this::mapToResponse)
//                 .collect(Collectors.toList());
//     }

//     @Transactional(readOnly = true)
//     public List<OrderResponse> getAllOrdersForAdmin() {
//         return orderRepository.findAllWithItems()
//                 .stream()
//                 .map(this::mapToResponse)
//                 .collect(Collectors.toList());
//     }

//     @Transactional
//     public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
//         Order order = orderRepository.findById(orderId)
//                 .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

//         OrderStatus current = order.getOrderStatus();
//         if (current == newStatus) {
//             return mapToResponse(order); // nothing to do, no duplicate email
//         }

//         if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
//             throw new CartBusinessException(
//                     "An order that is " + current + " cannot be changed to " + newStatus + ".");
//         }

//         boolean cod = "COD".equalsIgnoreCase(order.getPaymentMethod());
//         boolean paid = order.getPaymentStatus() == PaymentStatus.PAID;
//         boolean needsPayment = newStatus == OrderStatus.CONFIRMED
//                 || newStatus == OrderStatus.SHIPPED
//                 || newStatus == OrderStatus.DELIVERED;
//         if (needsPayment && !cod && !paid) {
//             throw new CartBusinessException(
//                     "This card order has not been paid yet, so it cannot be moved to " + newStatus + ".");
//         }

//         if (newStatus == OrderStatus.CANCELLED) {
//             cancelInternal(order);
//         } else {
//             order.setOrderStatus(newStatus);
//             if (newStatus == OrderStatus.DELIVERED && cod) {
//                 order.setPaymentStatus(PaymentStatus.PAID); // cash collected on delivery
//             }
//         }

//         Order saved = orderRepository.save(order);
//         events.publishEvent(new OrderEmailEvent(saved.getOrderId(),
//                 newStatus == OrderStatus.CANCELLED ? OrderEmailEvent.Type.CANCELLED
//                         : OrderEmailEvent.Type.STATUS_UPDATE));
//         return mapToResponse(saved);
//     }

//     @Transactional
//     public OrderResponse cancelMyOrder(Long orderId) {
//         User user = getAuthenticatedUser();
//         Order order = orderRepository.findById(orderId)
//                 .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

//         if (!order.getUser().getUserId().equals(user.getUserId())) {
//             throw new org.springframework.security.access.AccessDeniedException(
//                     "You are not authorized to cancel this order");
//         }

//         if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
//             throw new CartBusinessException(
//                     "This order can no longer be cancelled - it's already " +
//                             order.getOrderStatus().toString().toLowerCase() +
//                             ". Contact support for a return instead.");
//         }

//         cancelInternal(order);
//         return mapToResponse(orderRepository.save(order));
//     }

//     /**
//      * Shared cancel logic: restock, mark cancelled, and handle money/coupon.
//      * - PAID -> REFUND_PENDING (you still need to issue the Razorpay refund)
//      * - unpaid -> release the promo code so the customer can use it again
//      */
//     private void cancelInternal(Order order) {
//         if (order.getOrderStatus() == OrderStatus.CANCELLED)
//             return;

//         restoreStock(order);
//         order.setOrderStatus(OrderStatus.CANCELLED);

//         if (order.getPaymentStatus() == PaymentStatus.PAID) {
//             order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
//         } else if (order.getPromoCode() != null && order.getUser() != null) {
//             promoService.release(order.getUser().getEmail(), order.getPromoCode());
//         }
//     }

//     /**
//      * Abandoned card checkouts would otherwise hold stock forever.
//      * Requires @EnableScheduling on your application class and the repository
//      * method shown in PATCHES_BACKEND.md.
//      */
//     @Scheduled(fixedDelay = 600_000)
//     @Transactional
//     public void cancelExpiredUnpaidCardOrders() {
//         LocalDateTime cutoff = LocalDateTime.now().minusMinutes(UNPAID_ORDER_TTL_MINUTES);
//         orderRepository
//                 .findByOrderStatusAndPaymentStatusAndCreatedAtBefore(OrderStatus.PENDING, PaymentStatus.PENDING, cutoff)
//                 .forEach(o -> {
//                     cancelInternal(o);
//                     orderRepository.save(o);
//                 });
//     }

//     private String buildVariantLabel(ProductVariant variant) {
//         StringBuilder sb = new StringBuilder();
//         if (variant.getSize() != null && !variant.getSize().isBlank()) {
//             sb.append("Size: ").append(variant.getSize());
//         }
//         if (variant.getColor() != null && !variant.getColor().isBlank()) {
//             if (sb.length() > 0)
//                 sb.append(", ");
//             sb.append("Color: ").append(variant.getColor());
//         }
//         return sb.toString();
//     }

//     private void syncStockStatus(Product product) {
//         product.setStatus(product.getStock() <= 0
//                 ? com.example.MyProject.Enum.ProductStatus.OUT_OF_STOCK
//                 : com.example.MyProject.Enum.ProductStatus.IN_STOCK);
//     }

//     private void restoreStock(Order order) {
//         order.getOrderItems().stream()
//                 .sorted((a, b) -> {
//                     int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
//                     if (byProduct != 0)
//                         return byProduct;
//                     Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
//                     Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
//                     return Long.compare(va, vb);
//                 })
//                 .forEach(item -> {
//                     if (item.getVariant() != null) {
//                         ProductVariant variant = productVariantRepository
//                                 .findByIdForUpdate(item.getVariant().getVariantId())
//                                 .orElse(item.getVariant());
//                         variant.setStock(variant.getStock() + item.getQuantity());
//                         productVariantRepository.save(variant);

//                         Product parent = productRepository.findByIdForUpdate(item.getProduct().getProductId())
//                                 .orElse(item.getProduct());
//                         int totalVariantStock = parent.getVariants().stream()
//                                 .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock()
//                                         : v.getStock())
//                                 .sum();
//                         parent.setStock(totalVariantStock);
//                         syncStockStatus(parent);
//                         productRepository.save(parent);
//                     } else {
//                         Product product = productRepository.findByIdForUpdate(item.getProduct().getProductId())
//                                 .orElse(item.getProduct());
//                         product.setStock(product.getStock() + item.getQuantity());
//                         syncStockStatus(product);
//                         productRepository.save(product);
//                     }
//                 });
//     }

//     private User getAuthenticatedUser() {
//         Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//         String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername()
//                 : principal.toString();
//         return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
//     }

//     private OrderResponse mapToResponse(Order order) {
//         OrderResponse res = new OrderResponse();
//         res.setOrderId(order.getOrderId());
//         res.setOrderDate(order.getOrderDate());
//         res.setTotalAmount(order.getTotalAmount());
//         res.setOrderStatus(order.getOrderStatus().name());
//         res.setShippingAddress(order.getShippingAddress());
//         res.setPaymentStatus(order.getPaymentStatus().name());
//         res.setPaymentMethod(order.getPaymentMethod());
//         res.setPromoCode(order.getPromoCode());
//         res.setDiscountAmount(order.getDiscountAmount());
//         res.setPhoneNumber(order.getPhoneNumber());
//         if (order.getUser() != null) {
//             res.setCustomerName(order.getUser().getUserName());
//             res.setCustomerEmail(order.getUser().getEmail());
//         }
//         res.setItems(order.getOrderItems().stream().map(item -> {
//             OrderItemResponse iRes = new OrderItemResponse();
//             iRes.setProductId(item.getProduct().getProductId());
//             iRes.setProductName(item.getProduct().getName());
//             iRes.setQuantity(item.getQuantity());
//             iRes.setPriceAtPurchase(item.getPrice());
//             iRes.setImageUrl(item.getProduct().getImageUrl());
//             iRes.setVariantLabel(item.getVariantLabel());
//             return iRes;
//         }).collect(Collectors.toList()));

//         return res;
//     }

//     @Transactional(readOnly = true)
//     public List<RecentActivityResponse> getRecentActivity(int limit) {
//         return orderRepository.findTop30ByOrderStatusInOrderByOrderDateDesc(
//                 List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED)).stream()
//                 .filter(order -> order.getUser() != null && !order.getOrderItems().isEmpty())
//                 .map(order -> RecentActivityResponse.builder()
//                         .buyerName(anonymousBuyer(order))
//                         .productName(order.getOrderItems().get(0).getProduct().getName())
//                         .build())
//                 .distinct()
//                 .limit(limit)
//                 .collect(Collectors.toList());
//     }

//     // private String firstNameOnly(String fullName) {
//     //     if (fullName == null || fullName.isBlank())
//     //         return "Someone";
//     //     return fullName.trim().split("\\s+")[0];
//     // }

//     private String anonymousBuyer(Order order) {
//         String city = order.getCity();
//         if (city == null || city.isBlank())
//             return "Someone";
//         String trimmed = city.trim();
//         String pretty = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
//         return "Someone in " + pretty;
//     }

//     private String getMessage(String code, Object... args) {
//         return messageSource.getMessage(code, args, Locale.getDefault());
//     }
// }

package com.example.MyProject.Services;

import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.*;
import com.example.MyProject.Order.dto.*;
import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    /** Unpaid card orders hold stock; release it after this long. */
    private static final int UNPAID_ORDER_TTL_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final PricingService pricing;
    private final PromoService promoService;
    private final ApplicationEventPublisher events;
    // CHANGED: used by the scheduler so every expired order gets its own short
    // transaction + row lock.
    // Spring Boot creates this bean automatically.
    private final TransactionTemplate tx;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    @Transactional
    public OrderResponse checkout(OrderRequest request) {
        User user = getAuthenticatedUser();
        boolean cod = "COD".equalsIgnoreCase(request.getPaymentMethod());

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        // COD is confirmed straight away; card orders stay PENDING until payment is
        // verified.
        order.setOrderStatus(cod ? OrderStatus.CONFIRMED : OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setPhoneNumber(PhoneUtil.normalize(request.getPhoneNumber()));
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setZipCode(request.getZipCode());
        order.setShippingAddress(String.format("%s, %s, %s - %s",
                request.getShippingAddress(), request.getCity(), request.getState(), request.getZipCode()));

        List<OrderItem> orderItems = new ArrayList<>();
        Cart cart = null;

        if (request.getProductId() != null) {
            // PATH A: Buy Now (bypasses the cart)
            int qty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
            orderItems.add(buildOrderItem(order, request.getProductId(), request.getVariantId(), qty));
        } else {
            // PATH B: standard cart checkout
            cart = cartRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.not.found")));

            if (cart.getItems().isEmpty()) {
                throw new CartBusinessException(getMessage("order.cart.empty"));
            }

            // Consistent lock ordering to avoid deadlocks
            List<CartItem> sortedItems = cart.getItems().stream()
                    .sorted((a, b) -> {
                        int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
                        if (byProduct != 0)
                            return byProduct;
                        Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
                        Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
                        return Long.compare(va, vb);
                    })
                    .collect(Collectors.toList());

            for (CartItem ci : sortedItems) {
                Long variantId = ci.getVariant() != null ? ci.getVariant().getVariantId() : null;
                orderItems.add(buildOrderItem(order, ci.getProduct().getProductId(), variantId, ci.getQuantity()));
            }
        }

        // Prices always come from PricingService, never from the client or a stale cart
        // row.
        double subtotal = 0.0;
        for (OrderItem item : orderItems) {
            subtotal += item.getPrice() * item.getQuantity();
        }
        subtotal = pricing.round2(subtotal);

        // Promo is validated and applied here, on the server.
        PromoService.PromoResult promo = promoService.resolve(request.getPromoCode(), user.getEmail(), subtotal);
        double discount = promo != null ? promo.discount() : 0.0;

        order.setTotalAmount(pricing.round2(subtotal - discount));
        order.setPromoCode(promo != null ? promo.code() : null);
        order.setDiscountAmount(discount);

        // Card orders clear the cart only after payment succeeds
        // (PaymentService.markPaid).
        if (cart != null && cod) {
            cart.getItems().clear();
            cart.setTotalAmount(0.0);
            cartRepository.save(cart);
        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // Reserve the code now; it is released again if this order is cancelled unpaid.
        if (promo != null) {
            promoService.record(user.getEmail(), promo.code());
        }

        // CHANGED: COD orders are confirmed immediately, so send the confirmation email
        // now.
        // (Card orders get theirs from PaymentService.markPaid once payment is
        // verified.)
        if (cod) {
            events.publishEvent(new OrderEmailEvent(savedOrder.getOrderId(), OrderEmailEvent.Type.CONFIRMATION));
        }

        return mapToResponse(savedOrder);
    }

    /**
     * Locks and decrements stock for one line and prices it.
     * Used by BOTH Buy Now and cart checkout so they cannot diverge.
     */
    private OrderItem buildOrderItem(Order order, Long productId, Long variantId, int qty) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setQuantity(qty);

        if (variantId != null) {
            ProductVariant variant = productVariantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            getMessage("order.product.outOfStock", "Product Variant")));

            Product product = variant.getProduct();
            if (!product.getProductId().equals(productId)) {
                throw new CartBusinessException("Selected option does not belong to this product.");
            }
            if (variant.getStock() < qty) {
                throw new CartBusinessException(getMessage("order.product.outOfStock",
                        product.getName() + " (" + buildVariantLabel(variant) + ")"));
            }

            variant.setStock(variant.getStock() - qty);
            productVariantRepository.save(variant);

            item.setProduct(product);
            item.setVariant(variant);
            item.setVariantLabel(buildVariantLabel(variant));
            item.setPrice(pricing.unitPrice(product, variant));

            Product parent = productRepository.findByIdForUpdate(productId).orElse(product);
            int totalVariantStock = parent.getVariants().stream()
                    .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock() : v.getStock())
                    .sum();
            parent.setStock(totalVariantStock);
            syncStockStatus(parent);
            productRepository.save(parent);
        } else {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException(getMessage("order.product.outOfStock", "Product")));

            if (!product.getVariants().isEmpty()) {
                throw new CartBusinessException("Please select a size/color option for this product.");
            }
            if (product.getStock() < qty) {
                throw new CartBusinessException(getMessage("order.product.outOfStock", product.getName()));
            }

            product.setStock(product.getStock() - qty);
            syncStockStatus(product);
            productRepository.save(product);

            item.setProduct(product);
            item.setPrice(pricing.unitPrice(product, null));
        }
        return item;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrderHistory() {
        User user = getAuthenticatedUser();
        return orderRepository.findByUserIdWithItems(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllWithItems()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        // CHANGED: findByIdForUpdate (row lock) instead of findById, so a status change
        // can't
        // overwrite a payment that lands at the same moment (webhook / browser
        // callback).
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus current = order.getOrderStatus();
        if (current == newStatus) {
            return mapToResponse(order); // nothing to do, no duplicate email
        }

        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw new CartBusinessException(
                    "An order that is " + current + " cannot be changed to " + newStatus + ".");
        }

        boolean cod = "COD".equalsIgnoreCase(order.getPaymentMethod());
        boolean paid = order.getPaymentStatus() == PaymentStatus.PAID;
        boolean needsPayment = newStatus == OrderStatus.CONFIRMED
                || newStatus == OrderStatus.SHIPPED
                || newStatus == OrderStatus.DELIVERED;
        if (needsPayment && !cod && !paid) {
            throw new CartBusinessException(
                    "This card order has not been paid yet, so it cannot be moved to " + newStatus + ".");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            cancelInternal(order);
        } else {
            order.setOrderStatus(newStatus);
            if (newStatus == OrderStatus.DELIVERED && cod) {
                order.setPaymentStatus(PaymentStatus.PAID); // cash collected on delivery
            }
        }

        Order saved = orderRepository.save(order);
        events.publishEvent(new OrderEmailEvent(saved.getOrderId(),
                newStatus == OrderStatus.CANCELLED ? OrderEmailEvent.Type.CANCELLED
                        : OrderEmailEvent.Type.STATUS_UPDATE));
        return mapToResponse(saved);
    }

    @Transactional
    public OrderResponse cancelMyOrder(Long orderId) {
        User user = getAuthenticatedUser();
        // CHANGED: row lock, same reason as updateOrderStatus.
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to cancel this order");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new CartBusinessException(
                    "This order can no longer be cancelled - it's already " +
                            order.getOrderStatus().toString().toLowerCase() +
                            ". Contact support for a return instead.");
        }

        cancelInternal(order);
        Order saved = orderRepository.save(order);
        // CHANGED: customer-initiated cancels now send the same email as admin cancels.
        // Delete this line if you don't want that.
        events.publishEvent(new OrderEmailEvent(saved.getOrderId(), OrderEmailEvent.Type.CANCELLED));
        return mapToResponse(saved);
    }

    /**
     * Shared cancel logic: restock, mark cancelled, and handle money/coupon.
     * - PAID -> REFUND_PENDING (you still need to issue the Razorpay refund)
     * - unpaid -> release the promo code so the customer can use it again
     */
    private void cancelInternal(Order order) {
        if (order.getOrderStatus() == OrderStatus.CANCELLED)
            return;

        restoreStock(order);
        order.setOrderStatus(OrderStatus.CANCELLED);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        } else if (order.getPromoCode() != null && order.getUser() != null) {
            promoService.release(order.getUser().getEmail(), order.getPromoCode());
        }
    }

    /**
     * Abandoned card checkouts would otherwise hold stock forever.
     * Requires @EnableScheduling on your application class.
     *
     * CHANGED: the old version loaded all expired orders with no lock and saved
     * them later, so a
     * payment that committed in between could be overwritten by this job's stale
     * copy. Now each
     * order is locked and re-checked inside its own transaction before being
     * cancelled.
     * NOTE: this method is deliberately NOT @Transactional.
     */
    @Scheduled(fixedDelay = 600_000)
    public void cancelExpiredUnpaidCardOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(UNPAID_ORDER_TTL_MINUTES);

        List<Long> expiredIds = orderRepository
                .findByOrderStatusAndPaymentStatusAndCreatedAtBefore(OrderStatus.PENDING, PaymentStatus.PENDING, cutoff)
                .stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        for (Long id : expiredIds) {
            try {
                tx.executeWithoutResult(status -> orderRepository.findByIdForUpdate(id).ifPresent(o -> {
                    // Re-check after taking the lock: a payment may have landed in the meantime.
                    if (o.getOrderStatus() == OrderStatus.PENDING
                            && o.getPaymentStatus() == PaymentStatus.PENDING) {
                        cancelInternal(o);
                        orderRepository.save(o);
                        log.info("Cancelled expired unpaid card order {}", id);
                    }
                }));
            } catch (Exception e) {
                // One bad order must not stop the rest; it will be retried on the next run.
                log.warn("Could not cancel expired order {}: {}", id, e.getMessage());
            }
        }
    }

    private String buildVariantLabel(ProductVariant variant) {
        StringBuilder sb = new StringBuilder();
        if (variant.getSize() != null && !variant.getSize().isBlank()) {
            sb.append("Size: ").append(variant.getSize());
        }
        if (variant.getColor() != null && !variant.getColor().isBlank()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append("Color: ").append(variant.getColor());
        }
        return sb.toString();
    }

    private void syncStockStatus(Product product) {
        product.setStatus(product.getStock() <= 0
                ? com.example.MyProject.Enum.ProductStatus.OUT_OF_STOCK
                : com.example.MyProject.Enum.ProductStatus.IN_STOCK);
    }

    private void restoreStock(Order order) {
        order.getOrderItems().stream()
                // CHANGED: skip lines whose product no longer exists (the sort below would NPE
                // on null).
                .filter(i -> i.getProduct() != null)
                .sorted((a, b) -> {
                    int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
                    if (byProduct != 0)
                        return byProduct;
                    Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
                    Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
                    return Long.compare(va, vb);
                })
                .forEach(item -> {
                    if (item.getVariant() != null) {
                        ProductVariant variant = productVariantRepository
                                .findByIdForUpdate(item.getVariant().getVariantId())
                                .orElse(item.getVariant());
                        variant.setStock(variant.getStock() + item.getQuantity());
                        productVariantRepository.save(variant);

                        Product parent = productRepository.findByIdForUpdate(item.getProduct().getProductId())
                                .orElse(item.getProduct());
                        int totalVariantStock = parent.getVariants().stream()
                                .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock()
                                        : v.getStock())
                                .sum();
                        parent.setStock(totalVariantStock);
                        syncStockStatus(parent);
                        productRepository.save(parent);
                    } else {
                        Product product = productRepository.findByIdForUpdate(item.getProduct().getProductId())
                                .orElse(item.getProduct());
                        product.setStock(product.getStock() + item.getQuantity());
                        syncStockStatus(product);
                        productRepository.save(product);
                    }
                });
    }

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername()
                : principal.toString();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setOrderId(order.getOrderId());
        res.setOrderDate(order.getOrderDate());
        res.setTotalAmount(order.getTotalAmount());
        res.setOrderStatus(order.getOrderStatus().name());
        res.setShippingAddress(order.getShippingAddress());
        res.setPaymentStatus(order.getPaymentStatus().name());
        res.setPaymentMethod(order.getPaymentMethod());
        res.setPromoCode(order.getPromoCode());
        res.setDiscountAmount(order.getDiscountAmount());
        res.setPhoneNumber(order.getPhoneNumber());
        if (order.getUser() != null) {
            res.setCustomerName(order.getUser().getUserName());
            res.setCustomerEmail(order.getUser().getEmail());
        }
        res.setItems(order.getOrderItems().stream().map(item -> {
            OrderItemResponse iRes = new OrderItemResponse();
            // CHANGED: null-safe, so one missing product can never break a whole order
            // list.
            Product p = item.getProduct();
            iRes.setProductId(p != null ? p.getProductId() : null);
            iRes.setProductName(p != null ? p.getName() : "Removed product");
            iRes.setQuantity(item.getQuantity());
            iRes.setPriceAtPurchase(item.getPrice());
            iRes.setImageUrl(p != null ? p.getImageUrl() : null);
            iRes.setVariantLabel(item.getVariantLabel());
            return iRes;
        }).collect(Collectors.toList()));

        return res;
    }

    @Transactional(readOnly = true)
    public List<RecentActivityResponse> getRecentActivity(int limit) {
        return orderRepository.findTop30ByOrderStatusInOrderByOrderDateDesc(
                List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED)).stream()
                // CHANGED: also require the first item to still have a product.
                .filter(order -> order.getUser() != null
                        && !order.getOrderItems().isEmpty()
                        && order.getOrderItems().get(0).getProduct() != null)
                .map(order -> RecentActivityResponse.builder()
                        .buyerName(anonymousBuyer(order))
                        .productName(order.getOrderItems().get(0).getProduct().getName())
                        .build())
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String anonymousBuyer(Order order) {
        String city = order.getCity();
        if (city == null || city.isBlank())
            return "Someone";
        String trimmed = city.trim();
        String pretty = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
        return "Someone in " + pretty;
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, Locale.getDefault());
    }
}