package com.example.MyProject.Controller;

import com.example.MyProject.Enum.OrderStatus;
import com.example.MyProject.Order.dto.OrderResponse;
import com.example.MyProject.Services.OrderService;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @CrossOrigin removed: CORS is handled globally in SecurityConfig.
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> allOrders = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(new ApiResponse<>(true, allOrders, "All orders fetched successfully"));
    }

    // Which changes are allowed is decided in OrderService.updateOrderStatus.
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) String trackingNumber) {

        OrderResponse response = orderService.updateOrderStatus(orderId, status, carrier, trackingNumber);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Order status updated to " + status));
    }

    // NEW: sends the money back for a cancelled order that was paid online (full refund through Razorpay).
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<ApiResponse<OrderResponse>> refund(@PathVariable Long orderId) {
        OrderResponse response = orderService.refundOrder(orderId);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Refund issued for order #" + orderId));
    }
}