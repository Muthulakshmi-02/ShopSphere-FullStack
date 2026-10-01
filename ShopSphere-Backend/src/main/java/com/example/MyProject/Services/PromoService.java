package com.example.MyProject.Services;

import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Models.Coupon;
import com.example.MyProject.Models.UserCoupon;
import com.example.MyProject.Repository.CouponRepository;
import com.example.MyProject.Repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Server-side promo handling. The frontend only DISPLAYS a discount;
 * this is what actually decides it at checkout.
 *
 * Assumes (matching what your validate-promo endpoint returns to the UI):
 *   - coupon.getType()  -> "percent" or "flat" (String or enum, compared case-insensitively)
 *   - coupon.getValue() -> for percent: a fraction (0.10 = 10%), or a whole
 *                          number (10 = 10%) - both handled; for flat: rupees
 * If your Coupon has min-order / expiry / usage-limit fields, check them here.
 */
@Service
@RequiredArgsConstructor
public class PromoService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PricingService pricing;

    public record PromoResult(String code, double discount) {}

    /** Returns null when no code was supplied; throws if a supplied code is unusable. */
    @Transactional(readOnly = true)
    public PromoResult resolve(String rawCode, String userEmail, double subtotal) {
        if (rawCode == null || rawCode.isBlank()) return null;
        String code = rawCode.trim().toUpperCase();

        if (userCouponRepository.existsByUserEmailAndCouponCode(userEmail, code)) {
            throw new CartBusinessException("You have already used this promo code on a previous order.");
        }

        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new CartBusinessException("Invalid or expired promo code."));

        Number rawValue = coupon.getValue();
        double value = rawValue == null ? 0.0 : rawValue.doubleValue();
        String type = String.valueOf(coupon.getType()).toLowerCase();

        double discount = type.equals("percent")
                ? subtotal * (value > 1 ? value / 100.0 : value)
                : value;

        discount = Math.max(0.0, Math.min(discount, subtotal));
        return new PromoResult(code, pricing.round2(discount));
    }

    @Transactional
    public void record(String userEmail, String code) {
        userCouponRepository.save(UserCoupon.builder()
                .userEmail(userEmail)
                .couponCode(code)
                .usedAt(LocalDateTime.now())
                .build());
    }

    /** Called when an UNPAID order is cancelled/expired so the customer can reuse the code. */
    @Transactional
    public void release(String userEmail, String code) {
        userCouponRepository.deleteByUserEmailAndCouponCode(userEmail, code);
    }
}