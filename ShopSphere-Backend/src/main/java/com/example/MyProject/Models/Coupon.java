package com.example.MyProject.Models;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String type; // "percent" or "flat"

    // percent: store a FRACTION (0.10 = 10%). flat: rupees (100.0 = ₹100).
    @Column(nullable = false)
      private Double value;

    // Without @Builder.Default, Coupon.builder().build() produced active=false.
    @Builder.Default
    private boolean active = true;
}