package com.example.MyProject.Models;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Store codes in UPPERCASE (input is uppercased before lookup; lookup is also case-insensitive).
    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String type; // exactly "percent" or "flat"

    // percent: a FRACTION (0.10 = 10%). flat: rupees (100.0 = ₹100).
    @Column(nullable = false)
    private Double value;

    @Builder.Default
    private boolean active = true;

    // NEW, both optional (null = no rule). Hibernate adds the columns on next start;
    // existing coupons keep working exactly as before.
    @Column(name = "min_order_amount")
    private Double minOrderAmount;   // e.g. 500.0 = order must be at least ₹500

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // e.g. 2026-12-31T23:59:59 = unusable after that moment
}