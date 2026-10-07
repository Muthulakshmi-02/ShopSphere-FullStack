package com.example.MyProject.Controller;

import com.example.MyProject.Order.dto.OrderRequest;
import com.example.MyProject.Order.dto.OrderResponse;
import com.example.MyProject.Order.dto.RecentActivityResponse;
import com.example.MyProject.Services.OrderService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// The per-controller @CrossOrigin(origins = "http://localhost:4500") was removed: CORS is
// configured once in SecurityConfig (app.cors.allowed-origins), so there is a single place
// to change when you deploy.
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MessageSource messageSource;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.checkout(request);
        String msg = messageSource.getMessage("order.placed.success", null, LocaleContextHolder.getLocale());
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

    // Public: powers the storefront's live ticker. First name + product name only.
    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<RecentActivityResponse>>> getRecentActivity() {
        List<RecentActivityResponse> activity = orderService.getRecentActivity(10);
        return ResponseEntity.ok(new ApiResponse<>(true, activity, "Fetched recent activity"));
    }
}