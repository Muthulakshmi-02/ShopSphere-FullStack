package com.example.MyProject.Coupon.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 20, message = "Code must be between 3 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code must contain only letters, numbers, hyphens, and underscores")
    private String code;

    @NotBlank(message = "Discount type is required ('percent' or 'flat')")
    @Pattern(regexp = "^(?i)(percent|flat)$", message = "Type must be 'percent' or 'flat'")
    private String type;

    @NotNull(message = "Discount value is required")
    @Positive(message = "Discount value must be greater than 0")
    private Double value;

    @Builder.Default
    private boolean active = true;

    @PositiveOrZero(message = "Minimum order amount cannot be negative")
    private Double minOrderAmount;

    private LocalDateTime expiresAt;
}
