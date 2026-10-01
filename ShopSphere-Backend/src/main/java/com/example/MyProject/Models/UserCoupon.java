package com.example.MyProject.Models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_coupon",
                columnNames = {"user_email", "coupon_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Explicit names = the same columns Spring would have generated
    // (user_email / coupon_code), so no data migration is needed.
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "coupon_code", nullable = false)
    private String couponCode;

    @Builder.Default
    private LocalDateTime usedAt = LocalDateTime.now();
}