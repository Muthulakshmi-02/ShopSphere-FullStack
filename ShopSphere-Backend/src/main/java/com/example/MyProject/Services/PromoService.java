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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The ONE place that decides whether a promo code works and how much it takes off.
 * Used by checkout (resolve) and by the cart's "Apply" button (preview), so the preview and
 * the real order can no longer disagree.
 *
 * Coupon rules: type is exactly "percent" or "flat"; percent value is a fraction (0.10 = 10%),
 * a whole number up to 100 is also read as a percentage (10 = 10%); flat value is rupees.
 */
@Service
@RequiredArgsConstructor
public class PromoService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PricingService pricing;

    public record PromoResult(String code, double discount) {}

    // ------------------------------------------------------------------
    // Checkout: validates and calculates the real discount
    // ------------------------------------------------------------------

    /** Returns null when no code was supplied; throws if a supplied code cannot be used. */
    @Transactional(readOnly = true)
    public PromoResult resolve(String rawCode, String userEmail, double subtotal) {
        if (rawCode == null || rawCode.isBlank()) return null;
        String code = rawCode.trim().toUpperCase(Locale.ROOT);

        if (userCouponRepository.existsByUserEmailAndCouponCode(userEmail, code)) {
            throw new CartBusinessException("You have already used this promo code on a previous order.");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new CartBusinessException("Invalid or expired promo code."));

        if (isExpired(coupon)) {
            throw new CartBusinessException("This promo code has expired.");
        }
        if (!isWellFormed(coupon)) {
            // e.g. type typed as "percentage", or a zero/negative value. Better to refuse than
            // to give the wrong discount (the old code treated every non-"percent" type as flat).
            throw new CartBusinessException("This promo code is not set up correctly. Please contact support.");
        }
        if (coupon.getMinOrderAmount() != null && subtotal < coupon.getMinOrderAmount()) {
            throw new CartBusinessException(String.format(Locale.ROOT,
                    "This code needs a minimum order of ₹%.2f. Add ₹%.2f more to use it.",
                    coupon.getMinOrderAmount(), coupon.getMinOrderAmount() - subtotal));
        }

        double value = coupon.getValue();
        double discount = isPercent(coupon)
                ? subtotal * (value > 1 ? value / 100.0 : value)
                : value;

        discount = Math.max(0.0, Math.min(discount, subtotal));
        return new PromoResult(code, pricing.round2(discount));
    }

    // ------------------------------------------------------------------
    // Cart "Apply" button: same checks except the order-size rule (no subtotal here)
    // Returns the same JSON keys the frontend already reads: valid, used, type, value, message.
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> preview(String rawCode, String userEmail) {
        Map<String, Object> result = new HashMap<>();
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);

        if (userCouponRepository.existsByUserEmailAndCouponCode(userEmail, code)) {
            result.put("valid", false);
            result.put("used", true);
            result.put("message", "You have already used this promo code on a previous order.");
            return result;
        }

        Optional<Coupon> found = couponRepository.findByCodeIgnoreCaseAndActiveTrue(code);
        if (found.isEmpty() || isExpired(found.get()) || !isWellFormed(found.get())) {
            result.put("valid", false);
            result.put("used", false);
            result.put("message", "Invalid or expired promo code.");
            return result;
        }

        Coupon coupon = found.get();
        result.put("valid", true);
        result.put("used", false);
        result.put("type", coupon.getType().trim().toLowerCase(Locale.ROOT));
        result.put("value", coupon.getValue());
        if (coupon.getMinOrderAmount() != null) {
            result.put("minOrderAmount", coupon.getMinOrderAmount());
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Usage tracking
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean isExpired(Coupon coupon) {
        return coupon.getExpiresAt() != null && LocalDateTime.now().isAfter(coupon.getExpiresAt());
    }

    private boolean isPercent(Coupon coupon) {
        return "percent".equalsIgnoreCase(coupon.getType() == null ? "" : coupon.getType().trim());
    }

    private boolean isFlat(Coupon coupon) {
        return "flat".equalsIgnoreCase(coupon.getType() == null ? "" : coupon.getType().trim());
    }

    private boolean isWellFormed(Coupon coupon) {
        Double value = coupon.getValue();
        if (value == null || value <= 0) return false;
        if (isPercent(coupon)) return value <= 100;
        return isFlat(coupon);
    }
}