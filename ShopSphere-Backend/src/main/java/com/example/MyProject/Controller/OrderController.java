package com.example.MyProject.Controller;

import com.example.MyProject.Order.dto.OrderRequest;
import com.example.MyProject.Order.dto.OrderResponse;
import com.example.MyProject.Services.OrderService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4500", allowCredentials = "true")
public class OrderController {

    private final OrderService orderService;
    private final MessageSource messageSource;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.checkout(request);
        String msg = messageSource.getMessage("order.placed.success", null, Locale.getDefault());
        // Constructor Order: success, data, message
        return new ResponseEntity<>(new ApiResponse<>(true, response, msg), HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrderHistory() {
        List<OrderResponse> history = orderService.getUserOrderHistory();
        return ResponseEntity.ok(new ApiResponse<>(true, history, "Fetched history"));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(@PathVariable Long orderId) {
        OrderResponse response = orderService.cancelMyOrder(orderId);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Order cancelled"));
    }

    // Public (not @PreAuthorize) - this powers the storefront's live sales
    // ticker, which visitors see even before logging in. Returns only
    // buyer name + product name, never order IDs, totals, or addresses.
    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<com.example.MyProject.Order.dto.RecentActivityResponse>>> getRecentActivity() {
        var activity = orderService.getRecentActivity(10);
        return ResponseEntity.ok(new ApiResponse<>(true, activity, "Fetched recent activity"));
    }
}