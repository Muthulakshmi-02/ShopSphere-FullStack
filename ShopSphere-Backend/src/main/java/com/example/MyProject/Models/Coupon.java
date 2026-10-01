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

    @Column(nullable = false)
    private Double value; // e.g., 0.20 for 20% or 100.0 for ₹100

    private boolean active = true;
}