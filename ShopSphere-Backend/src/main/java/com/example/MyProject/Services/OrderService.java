////package com.example.MyProject.Services;
////
////import com.example.MyProject.Enum.PaymentStatus;
////import com.example.MyProject.Models.*;
////import com.example.MyProject.Order.dto.*;
////import com.example.MyProject.Enum.OrderStatus;
////import com.example.MyProject.Exception.CartBusinessException;
////import com.example.MyProject.Exception.ResourceNotFoundException;
////import com.example.MyProject.Repository.*;
////import lombok.RequiredArgsConstructor;
////import org.springframework.context.MessageSource;
////import org.springframework.security.core.context.SecurityContextHolder;
////import org.springframework.security.core.userdetails.UserDetails;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.time.LocalDateTime;
////import java.util.List;
////import java.util.Locale;
////import java.util.stream.Collectors;
////
////@Service
////@RequiredArgsConstructor
////public class OrderService {
////
////    private final OrderRepository orderRepository;
////    private final CartRepository cartRepository;
////    private final ProductRepository productRepository;
////    private final ProductVariantRepository productVariantRepository;
////    private final UserRepository userRepository; // Added this
////    private final MessageSource messageSource;
////
////    @Transactional(readOnly = true)
////    public OrderResponse checkout(OrderRequest request) {
////        User user = getAuthenticatedUser();
////
////        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
////                .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.not.found")));
////
////        if (cart.getItems().isEmpty()) {
////            throw new CartBusinessException(getMessage("order.cart.empty"));
////        }
////
////        Order order = new Order();
////        order.setUser(user);
////        order.setOrderDate(LocalDateTime.now());
////        order.setOrderStatus(OrderStatus.CONFIRMED);
////        order.setOrderDate(LocalDateTime.now());
////        order.setCreatedAt(LocalDateTime.now());
////        order.setTotalAmount(cart.getTotalAmount());
////        order.setPhoneNumber(request.getPhoneNumber());
////        order.setPaymentMethod(request.getPaymentMethod());
////        // Order is created PENDING regardless of method. Card payments only
////        // become PAID once /api/payments/verify confirms a (simulated)
////        // transaction; COD stays PENDING until payment is collected on delivery.
////        order.setPaymentStatus(PaymentStatus.PENDING);
////        order.setCity(request.getCity());
////        order.setState(request.getState());
////        order.setZipCode(request.getZipCode());
////
////        String fullAddress = String.format("%s, %s, %s - %s",
////                request.getShippingAddress(), request.getCity(), request.getState(), request.getZipCode());
////        order.setShippingAddress(fullAddress);
////
////        // Lock in a consistent order (product ID, then variant ID) before
////        // touching stock. Without this, two customers checking out the same
////        // low-stock item/variant at the same time could both read/pass the
////        // stock check before either commits, oversell it, and drive stock
////        // negative. Locking in a fixed order also prevents a deadlock
////        // between two orders that share items but would otherwise lock
////        // them in opposite orders.
////        List<CartItem> sortedItems = cart.getItems().stream()
////                .sorted((a, b) -> {
////                    int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
////                    if (byProduct != 0) return byProduct;
////                    Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
////                    Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
////                    return Long.compare(va, vb);
////                })
////                .collect(Collectors.toList());
////
////        List<OrderItem> orderItems = sortedItems.stream().map(cartItem -> {
////            OrderItem orderItem = new OrderItem();
////            orderItem.setOrder(order);
////            orderItem.setQuantity(cartItem.getQuantity());
////            orderItem.setPrice(cartItem.getPrice());
////
////            if (cartItem.getVariant() != null) {
////                // Variant product: stock lives on the variant row, not the
////                // base product - lock and decrement that instead.
////                ProductVariant variant = productVariantRepository.findByIdForUpdate(cartItem.getVariant().getVariantId())
////                        .orElseThrow(() -> new ResourceNotFoundException(
////                                getMessage("order.product.outOfStock", cartItem.getProduct().getName())));
////
////                if (variant.getStock() < cartItem.getQuantity()) {
////                    throw new CartBusinessException(getMessage("order.product.outOfStock",
////                            cartItem.getProduct().getName() + " (" + buildVariantLabel(variant) + ")"));
////                }
////
////                variant.setStock(variant.getStock() - cartItem.getQuantity());
////                productVariantRepository.save(variant);
////
////                orderItem.setProduct(cartItem.getProduct());
////                orderItem.setVariant(variant);
////                orderItem.setVariantLabel(buildVariantLabel(variant));
////
////                // Keep the base product's displayed stock/status in sync
////                // with its variants' combined stock, same as the admin
////                // create/update path already does.
////                Product parent = productRepository.findByIdForUpdate(cartItem.getProduct().getProductId())
////                        .orElse(cartItem.getProduct());
////                int totalVariantStock = parent.getVariants().stream()
////                        .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock() : v.getStock())
////                        .sum();
////                parent.setStock(totalVariantStock);
////                syncStockStatus(parent);
////                productRepository.save(parent);
////            } else {
////                Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getProductId())
////                        .orElseThrow(() -> new ResourceNotFoundException(getMessage("order.product.outOfStock", cartItem.getProduct().getName())));
////
////                if (product.getStock() < cartItem.getQuantity()) {
////                    throw new CartBusinessException(getMessage("order.product.outOfStock", product.getName()));
////                }
////
////                // Deduct Stock
////                product.setStock(product.getStock() - cartItem.getQuantity());
////                // BUG FIX: this used to only decrement stock without touching
////                // status, so a product could hit 0 stock from a purchase and
////                // still display as "In Stock" until an admin happened to edit
////                // it (only the admin create/update path kept status in sync).
////                syncStockStatus(product);
////                productRepository.save(product);
////
////                orderItem.setProduct(product);
////            }
////
////            return orderItem;
////        }).collect(Collectors.toList());
////
////        order.setOrderItems(orderItems);
////        Order savedOrder = orderRepository.save(order);
////
////        // Clear Cart logic
////        cart.getItems().clear();
////        cart.setTotalAmount(0.0);
////        cartRepository.save(cart);
////
////        return mapToResponse(savedOrder);
////    }
////
////    @Transactional(readOnly = true)
////    public List<OrderResponse> getUserOrderHistory() {
////        User user = getAuthenticatedUser();
////        return orderRepository.findByUser_UserIdOrderByOrderDateDesc(user.getUserId())
////                .stream()
////                .map(this::mapToResponse)
////                .collect(Collectors.toList());
////    }
////
////    @Transactional(readOnly = true)
////    public List<OrderResponse> getAllOrdersForAdmin() {
////        return orderRepository.findAllByOrderByOrderDateDesc()
////                .stream()
////                .map(this::mapToResponse)
////                .collect(Collectors.toList());
////    }
////
////    @Transactional
////    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
////        Order order = orderRepository.findById(orderId)
////                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
////
////        if (newStatus == OrderStatus.CANCELLED && order.getOrderStatus() != OrderStatus.CANCELLED) {
////            restoreStock(order);
////        }
////
////        order.setOrderStatus(newStatus);
////        return mapToResponse(orderRepository.save(order));
////    }
////
////    /**
////     * Customer-initiated cancellation. Unlike the admin's updateOrderStatus,
////     * this enforces that (a) the order actually belongs to the caller - an
////     * IDOR check, same class of bug as the payment-verify fix earlier - and
////     * (b) the order is still in a cancellable state. Once an order ships,
////     * self-service cancellation stops being safe (the package may already
////     * be in transit), so it's return/refund territory from there instead.
////     */
////    @Transactional
////    public OrderResponse cancelMyOrder(Long orderId) {
////        User user = getAuthenticatedUser();
////        Order order = orderRepository.findById(orderId)
////                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
////
////        if (!order.getUser().getUserId().equals(user.getUserId())) {
////            throw new org.springframework.security.access.AccessDeniedException(
////                    "You are not authorized to cancel this order");
////        }
////
////        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
////            throw new CartBusinessException(
////                    "This order can no longer be cancelled - it's already " +
////                    order.getOrderStatus().toString().toLowerCase() +
////                    ". Contact support for a return instead.");
////        }
////
////        restoreStock(order);
////        order.setOrderStatus(OrderStatus.CANCELLED);
////        return mapToResponse(orderRepository.save(order));
////    }
////
////    /**
////     * Restores stock for every item in a cancelled order, locking each
////     * product row first - same reasoning as checkout's locked decrement:
////     * without the lock, a cancellation restoring stock at the same instant
////     * another checkout is decrementing it could race and leave stock wrong.
////     */
////    /**
////     * Keeps a product's status truthful to its actual stock whenever stock
////     * changes here (checkout decrementing it, cancellation restoring it) -
////     * mirrors the same rule ProductService already applies on admin
////     * create/update, which this class doesn't share a base with.
////     */
////    /**
////     * Human-readable snapshot of a variant, e.g. "Size: 9, Color: Black" -
////     * stored on the OrderItem so order history stays meaningful even if the
////     * variant itself is later edited or removed from the product.
////     */
////    private String buildVariantLabel(ProductVariant variant) {
////        StringBuilder sb = new StringBuilder();
////        if (variant.getSize() != null && !variant.getSize().isBlank()) {
////            sb.append("Size: ").append(variant.getSize());
////        }
////        if (variant.getColor() != null && !variant.getColor().isBlank()) {
////            if (sb.length() > 0) sb.append(", ");
////            sb.append("Color: ").append(variant.getColor());
////        }
////        return sb.toString();
////    }
////
////    private void syncStockStatus(Product product) {
////        product.setStatus(product.getStock() <= 0
////                ? com.example.MyProject.Enum.ProductStatus.OUT_OF_STOCK
////                : com.example.MyProject.Enum.ProductStatus.IN_STOCK);
////    }
////
////    private void restoreStock(Order order) {
////        order.getOrderItems().stream()
////                .sorted((a, b) -> {
////                    int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
////                    if (byProduct != 0) return byProduct;
////                    Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
////                    Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
////                    return Long.compare(va, vb);
////                })
////                .forEach(item -> {
////                    if (item.getVariant() != null) {
////                        ProductVariant variant = productVariantRepository.findByIdForUpdate(item.getVariant().getVariantId())
////                                .orElse(item.getVariant());
////                        variant.setStock(variant.getStock() + item.getQuantity());
////                        productVariantRepository.save(variant);
////
////                        Product parent = productRepository.findByIdForUpdate(item.getProduct().getProductId())
////                                .orElse(item.getProduct());
////                        int totalVariantStock = parent.getVariants().stream()
////                                .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock() : v.getStock())
////                                .sum();
////                        parent.setStock(totalVariantStock);
////                        syncStockStatus(parent);
////                        productRepository.save(parent);
////                    } else {
////                        Product product = productRepository.findByIdForUpdate(item.getProduct().getProductId())
////                                .orElse(item.getProduct());
////                        product.setStock(product.getStock() + item.getQuantity());
////                        syncStockStatus(product);
////                        productRepository.save(product);
////                    }
////                });
////    }
////
////    // --- Helpers ---
////
////    private User getAuthenticatedUser() {
////        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
////        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
////        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
////    }
////
////    private OrderResponse mapToResponse(Order order) {
////        OrderResponse res = new OrderResponse();
////        res.setOrderId(order.getOrderId());
////        res.setOrderDate(order.getOrderDate());
////        res.setTotalAmount(order.getTotalAmount());
////        res.setOrderStatus(order.getOrderStatus().name());
////        res.setShippingAddress(order.getShippingAddress());
////        res.setPaymentStatus(order.getPaymentStatus().name());
////
////        res.setItems(order.getOrderItems().stream().map(item -> {
////            OrderItemResponse iRes = new OrderItemResponse();
////            iRes.setProductId(item.getProduct().getProductId());
////            iRes.setProductName(item.getProduct().getName());
////            iRes.setQuantity(item.getQuantity());
////            iRes.setPriceAtPurchase(item.getPrice());
////            iRes.setImageUrl(item.getProduct().getImageUrl());
////            iRes.setVariantLabel(item.getVariantLabel());
////            return iRes;
////        }).collect(Collectors.toList()));
////
////        return res;
////    }
////
////    /**
////     * Real recent-purchase feed for the storefront's live ticker (replaces
////     * the static illustrative list). Deliberately minimal - just buyer name
////     * + product name, nothing that identifies the order itself. Deduplicated
////     * by (buyer, product) pair so someone who bought the same thing twice
////     * recently doesn't show up twice in a short ticker list.
////     */
////    public List<RecentActivityResponse> getRecentActivity(int limit) {
////        return orderRepository.findTop30ByOrderByOrderDateDesc().stream()
////                .filter(order -> order.getUser() != null && !order.getOrderItems().isEmpty())
////                .map(order -> RecentActivityResponse.builder()
////                        .buyerName(order.getUser().getUserName())
////                        .productName(order.getOrderItems().get(0).getProduct().getName())
////                        .build())
////                .distinct()
////                .limit(limit)
////                .collect(Collectors.toList());
////    }
////
////    private String getMessage(String code, Object... args) {
////        return messageSource.getMessage(code, args, Locale.getDefault());
////    }
////}
//package com.example.MyProject.Services;
//
//import com.example.MyProject.Enum.PaymentStatus;
//import com.example.MyProject.Models.*;
//import com.example.MyProject.Order.dto.*;
//import com.example.MyProject.Enum.OrderStatus;
//import com.example.MyProject.Exception.CartBusinessException;
//import com.example.MyProject.Exception.ResourceNotFoundException;
//import com.example.MyProject.Repository.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.MessageSource;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Locale;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class OrderService {
//
//    private final OrderRepository orderRepository;
//    private final CartRepository cartRepository;
//    private final ProductRepository productRepository;
//    private final ProductVariantRepository productVariantRepository;
//    private final UserRepository userRepository;
//    private final MessageSource messageSource;
//
//    @Transactional
//    public OrderResponse checkout(OrderRequest request) {
//        User user = getAuthenticatedUser();
//
//        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
//                .orElseThrow(() -> new ResourceNotFoundException(getMessage("cart.not.found")));
//
//        if (cart.getItems().isEmpty()) {
//            throw new CartBusinessException(getMessage("order.cart.empty"));
//        }
//
//        Order order = new Order();
//        order.setUser(user);
//        order.setOrderDate(LocalDateTime.now());
//        order.setOrderStatus(OrderStatus.CONFIRMED);
//        order.setCreatedAt(LocalDateTime.now());
//        order.setTotalAmount(cart.getTotalAmount());
//        order.setPhoneNumber(request.getPhoneNumber());
//        order.setPaymentMethod(request.getPaymentMethod());
//        order.setPaymentStatus(PaymentStatus.PENDING);
//        order.setCity(request.getCity());
//        order.setState(request.getState());
//        order.setZipCode(request.getZipCode());
//
//        String fullAddress = String.format("%s, %s, %s - %s",
//                request.getShippingAddress(), request.getCity(), request.getState(), request.getZipCode());
//        order.setShippingAddress(fullAddress);
//
//        List<CartItem> sortedItems = cart.getItems().stream()
//                .sorted((a, b) -> {
//                    int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
//                    if (byProduct != 0) return byProduct;
//                    Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
//                    Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
//                    return Long.compare(va, vb);
//                })
//                .collect(Collectors.toList());
//
//        List<OrderItem> orderItems = sortedItems.stream().map(cartItem -> {
//            OrderItem orderItem = new OrderItem();
//            orderItem.setOrder(order);
//            orderItem.setQuantity(cartItem.getQuantity());
//            orderItem.setPrice(cartItem.getPrice());
//
//            if (cartItem.getVariant() != null) {
//                ProductVariant variant = productVariantRepository.findByIdForUpdate(cartItem.getVariant().getVariantId())
//                        .orElseThrow(() -> new ResourceNotFoundException(
//                                getMessage("order.product.outOfStock", cartItem.getProduct().getName())));
//
//                if (variant.getStock() < cartItem.getQuantity()) {
//                    throw new CartBusinessException(getMessage("order.product.outOfStock",
//                            cartItem.getProduct().getName() + " (" + buildVariantLabel(variant) + ")"));
//                }
//
//                variant.setStock(variant.getStock() - cartItem.getQuantity());
//                productVariantRepository.save(variant);
//
//                orderItem.setProduct(cartItem.getProduct());
//                orderItem.setVariant(variant);
//                orderItem.setVariantLabel(buildVariantLabel(variant));
//
//                Product parent = productRepository.findByIdForUpdate(cartItem.getProduct().getProductId())
//                        .orElse(cartItem.getProduct());
//                int totalVariantStock = parent.getVariants().stream()
//                        .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock() : v.getStock())
//                        .sum();
//                parent.setStock(totalVariantStock);
//                syncStockStatus(parent);
//                productRepository.save(parent);
//            } else {
//                Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getProductId())
//                        .orElseThrow(() -> new ResourceNotFoundException(getMessage("order.product.outOfStock", cartItem.getProduct().getName())));
//
//                if (product.getStock() < cartItem.getQuantity()) {
//                    throw new CartBusinessException(getMessage("order.product.outOfStock", product.getName()));
//                }
//
//                product.setStock(product.getStock() - cartItem.getQuantity());
//                syncStockStatus(product);
//                productRepository.save(product);
//
//                orderItem.setProduct(product);
//            }
//
//            return orderItem;
//        }).collect(Collectors.toList());
//
//        order.setOrderItems(orderItems);
//        Order savedOrder = orderRepository.save(order);
//
//        cart.getItems().clear();
//        cart.setTotalAmount(0.0);
//        cartRepository.save(cart);
//
//        return mapToResponse(savedOrder);
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderResponse> getUserOrderHistory() {
//        User user = getAuthenticatedUser();
//        return orderRepository.findByUser_UserIdOrderByOrderDateDesc(user.getUserId())
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderResponse> getAllOrdersForAdmin() {
//        return orderRepository.findAllByOrderByOrderDateDesc()
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Transactional
//    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
//
//        if (newStatus == OrderStatus.CANCELLED && order.getOrderStatus() != OrderStatus.CANCELLED) {
//            restoreStock(order);
//        }
//
//        order.setOrderStatus(newStatus);
//        return mapToResponse(orderRepository.save(order));
//    }
//
//    @Transactional
//    public OrderResponse cancelMyOrder(Long orderId) {
//        User user = getAuthenticatedUser();
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
//
//        if (!order.getUser().getUserId().equals(user.getUserId())) {
//            throw new org.springframework.security.access.AccessDeniedException(
//                    "You are not authorized to cancel this order");
//        }
//
//        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
//            throw new CartBusinessException(
//                    "This order can no longer be cancelled - it's already " +
//                            order.getOrderStatus().toString().toLowerCase() +
//                            ". Contact support for a return instead.");
//        }
//
//        restoreStock(order);
//        order.setOrderStatus(OrderStatus.CANCELLED);
//        return mapToResponse(orderRepository.save(order));
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
//    private void syncStockStatus(Product product) {
//        product.setStatus(product.getStock() <= 0
//                ? com.example.MyProject.Enum.ProductStatus.OUT_OF_STOCK
//                : com.example.MyProject.Enum.ProductStatus.IN_STOCK);
//    }
//
//    private void restoreStock(Order order) {
//        order.getOrderItems().stream()
//                .sorted((a, b) -> {
//                    int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
//                    if (byProduct != 0) return byProduct;
//                    Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
//                    Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
//                    return Long.compare(va, vb);
//                })
//                .forEach(item -> {
//                    if (item.getVariant() != null) {
//                        ProductVariant variant = productVariantRepository.findByIdForUpdate(item.getVariant().getVariantId())
//                                .orElse(item.getVariant());
//                        variant.setStock(variant.getStock() + item.getQuantity());
//                        productVariantRepository.save(variant);
//
//                        Product parent = productRepository.findByIdForUpdate(item.getProduct().getProductId())
//                                .orElse(item.getProduct());
//                        int totalVariantStock = parent.getVariants().stream()
//                                .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock() : v.getStock())
//                                .sum();
//                        parent.setStock(totalVariantStock);
//                        syncStockStatus(parent);
//                        productRepository.save(parent);
//                    } else {
//                        Product product = productRepository.findByIdForUpdate(item.getProduct().getProductId())
//                                .orElse(item.getProduct());
//                        product.setStock(product.getStock() + item.getQuantity());
//                        syncStockStatus(product);
//                        productRepository.save(product);
//                    }
//                });
//    }
//
//    private User getAuthenticatedUser() {
//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
//        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
//    }
//
//    private OrderResponse mapToResponse(Order order) {
//        OrderResponse res = new OrderResponse();
//        res.setOrderId(order.getOrderId());
//        res.setOrderDate(order.getOrderDate());
//        res.setTotalAmount(order.getTotalAmount());
//        res.setOrderStatus(order.getOrderStatus().name());
//        res.setShippingAddress(order.getShippingAddress());
//        res.setPaymentStatus(order.getPaymentStatus().name());
//
//        res.setItems(order.getOrderItems().stream().map(item -> {
//            OrderItemResponse iRes = new OrderItemResponse();
//            iRes.setProductId(item.getProduct().getProductId());
//            iRes.setProductName(item.getProduct().getName());
//            iRes.setQuantity(item.getQuantity());
//            iRes.setPriceAtPurchase(item.getPrice());
//            iRes.setImageUrl(item.getProduct().getImageUrl());
//            iRes.setVariantLabel(item.getVariantLabel());
//            return iRes;
//        }).collect(Collectors.toList()));
//
//        return res;
//    }
//
//    @Transactional(readOnly = true)
//    public List<RecentActivityResponse> getRecentActivity(int limit) {
//        return orderRepository.findTop30ByOrderByOrderDateDesc().stream()
//                .filter(order -> order.getUser() != null && !order.getOrderItems().isEmpty())
//                .map(order -> RecentActivityResponse.builder()
//                        .buyerName(order.getUser().getUserName())
//                        .productName(order.getOrderItems().get(0).getProduct().getName())
//                        .build())
//                .distinct()
//                .limit(limit)
//                .collect(Collectors.toList());
//    }
//
//    private String getMessage(String code, Object... args) {
//        return messageSource.getMessage(code, args, Locale.getDefault());
//    }
//}
package com.example.MyProject.Services;

import com.example.MyProject.Enum.PaymentStatus;
import com.example.MyProject.Models.*;
import com.example.MyProject.Order.dto.*;
import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    @Transactional
    public OrderResponse checkout(OrderRequest request) {
        User user = getAuthenticatedUser();
        boolean cod = "COD".equalsIgnoreCase(request.getPaymentMethod());

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        // COD is confirmed straight away; card orders stay PENDING until payment is verified.
        order.setOrderStatus(cod ? OrderStatus.CONFIRMED : OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setPhoneNumber(request.getPhoneNumber());
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
                        if (byProduct != 0) return byProduct;
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

        // Prices always come from PricingService, never from the client or a stale cart row.
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

        if (cart != null) {
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
                    .orElseThrow(() -> new ResourceNotFoundException(getMessage("order.product.outOfStock", "Product Variant")));

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
                    .orElseThrow(() -> new ResourceNotFoundException(getMessage("order.product.outOfStock", "Product")));

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
        return orderRepository.findByUser_UserIdOrderByOrderDateDesc(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByOrderDateDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (newStatus == OrderStatus.CANCELLED) {
            cancelInternal(order);
        } else {
            order.setOrderStatus(newStatus);
        }
        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelMyOrder(Long orderId) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findById(orderId)
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
        return mapToResponse(orderRepository.save(order));
    }

    /**
     * Shared cancel logic: restock, mark cancelled, and handle money/coupon.
     *  - PAID    -> REFUND_PENDING (you still need to issue the Razorpay refund)
     *  - unpaid  -> release the promo code so the customer can use it again
     */
    private void cancelInternal(Order order) {
        if (order.getOrderStatus() == OrderStatus.CANCELLED) return;

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
     * Requires @EnableScheduling on your application class and the repository
     * method shown in PATCHES_BACKEND.md.
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void cancelExpiredUnpaidCardOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(UNPAID_ORDER_TTL_MINUTES);
        orderRepository
                .findByOrderStatusAndPaymentStatusAndCreatedAtBefore(OrderStatus.PENDING, PaymentStatus.PENDING, cutoff)
                .forEach(o -> {
                    cancelInternal(o);
                    orderRepository.save(o);
                });
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

    private void syncStockStatus(Product product) {
        product.setStatus(product.getStock() <= 0
                ? com.example.MyProject.Enum.ProductStatus.OUT_OF_STOCK
                : com.example.MyProject.Enum.ProductStatus.IN_STOCK);
    }

    private void restoreStock(Order order) {
        order.getOrderItems().stream()
                .sorted((a, b) -> {
                    int byProduct = Long.compare(a.getProduct().getProductId(), b.getProduct().getProductId());
                    if (byProduct != 0) return byProduct;
                    Long va = a.getVariant() != null ? a.getVariant().getVariantId() : -1L;
                    Long vb = b.getVariant() != null ? b.getVariant().getVariantId() : -1L;
                    return Long.compare(va, vb);
                })
                .forEach(item -> {
                    if (item.getVariant() != null) {
                        ProductVariant variant = productVariantRepository.findByIdForUpdate(item.getVariant().getVariantId())
                                .orElse(item.getVariant());
                        variant.setStock(variant.getStock() + item.getQuantity());
                        productVariantRepository.save(variant);

                        Product parent = productRepository.findByIdForUpdate(item.getProduct().getProductId())
                                .orElse(item.getProduct());
                        int totalVariantStock = parent.getVariants().stream()
                                .mapToInt(v -> v.getVariantId().equals(variant.getVariantId()) ? variant.getStock() : v.getStock())
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
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
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

        res.setItems(order.getOrderItems().stream().map(item -> {
            OrderItemResponse iRes = new OrderItemResponse();
            iRes.setProductId(item.getProduct().getProductId());
            iRes.setProductName(item.getProduct().getName());
            iRes.setQuantity(item.getQuantity());
            iRes.setPriceAtPurchase(item.getPrice());
            iRes.setImageUrl(item.getProduct().getImageUrl());
            iRes.setVariantLabel(item.getVariantLabel());
            return iRes;
        }).collect(Collectors.toList()));

        return res;
    }

    @Transactional(readOnly = true)
    public List<RecentActivityResponse> getRecentActivity(int limit) {
        return orderRepository.findTop30ByOrderStatusInOrderByOrderDateDesc(
                        List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED)).stream()
                .filter(order -> order.getUser() != null && !order.getOrderItems().isEmpty())
                .map(order -> RecentActivityResponse.builder()
                        .buyerName(firstNameOnly(order.getUser().getUserName()))
                        .productName(order.getOrderItems().get(0).getProduct().getName())
                        .build())
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Someone";
        return fullName.trim().split("\\s+")[0];
    }

    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, Locale.getDefault());
    }
}