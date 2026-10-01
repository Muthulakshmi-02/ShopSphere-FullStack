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

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:4500", allowCredentials = "true")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> allOrders = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(new ApiResponse<>(true, allOrders, "All orders fetched successfully"));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {

        OrderResponse response = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Order status updated to " + status));
    }
}