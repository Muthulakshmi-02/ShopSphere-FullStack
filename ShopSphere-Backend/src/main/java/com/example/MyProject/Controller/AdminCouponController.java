package com.example.MyProject.Controller;

import com.example.MyProject.Coupon.dto.CouponRequest;
import com.example.MyProject.Coupon.dto.CouponResponse;
import com.example.MyProject.Services.AdminCouponService;
import com.example.MyProject.User.Dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final AdminCouponService couponService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        List<CouponResponse> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(new ApiResponse<>(true, coupons, "Coupons fetched successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCouponById(@PathVariable Long id) {
        CouponResponse coupon = couponService.getCouponById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, coupon, "Coupon fetched successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CouponRequest dto) {
        CouponResponse created = couponService.createCoupon(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, created, "Coupon created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest dto) {
        CouponResponse updated = couponService.updateCoupon(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, updated, "Coupon updated successfully"));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleCouponActive(@PathVariable Long id) {
        CouponResponse toggled = couponService.toggleCouponActive(id);
        return ResponseEntity.ok(new ApiResponse<>(true, toggled, "Coupon status updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Coupon deleted successfully"));
    }
}
