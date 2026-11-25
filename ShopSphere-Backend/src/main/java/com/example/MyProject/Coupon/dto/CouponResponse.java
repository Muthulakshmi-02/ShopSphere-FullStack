package com.example.MyProject.Coupon.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private Long id;
    private String code;
    private String type; // "percent" or "flat"
    private Double value; // raw value stored in DB
    private Double displayValue; // e.g. 10 for 10%, 100 for ₹100
    private boolean active;
    private Double minOrderAmount;
    private LocalDateTime expiresAt;
    private boolean expired;
    private long usageCount;
}
