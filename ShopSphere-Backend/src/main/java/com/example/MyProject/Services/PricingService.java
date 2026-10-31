// package com.example.MyProject.Services;

// import com.example.MyProject.Models.Product;
// import com.example.MyProject.Models.ProductVariant;
// import org.springframework.stereotype.Service;

// /**
//  * Single source of truth for what a customer pays per unit.
//  * Used by CartService (add / re-price) AND OrderService (cart + Buy Now),
//  * so the two flows can never disagree again.
//  * The formula is identical to the one that was in CartService.addProductToCart.
//  */
// @Service
// public class PricingService {

//     public double unitPrice(Product product, ProductVariant variant) {
//         double base = (variant != null && variant.getPriceOverride() != null)
//                 ? variant.getPriceOverride()
//                 : product.getPrice();

//         double finalPrice = base;
//         Integer discount = product.getDiscountPercentage();
//         if (discount != null && discount > 0) {
//             finalPrice = base - (base * discount / 100.0);
//         }
//         return round2(finalPrice);
//     }

//     public double round2(double value) {
//         return Math.round(value * 100.0) / 100.0;
//     }
// }

package com.example.MyProject.Services;

import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.ProductVariant;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single source of truth for what a customer pays per unit.
 * Used by CartService (add / re-price) AND OrderService (cart + Buy Now),
 * so the two flows can never disagree again.
 *
 * CHANGED: the maths now uses BigDecimal with HALF_UP rounding (the same rule Razorpay and
 * PaymentService use for paise), and a discount outside 0-100 is clamped, so a typo like 150%
 * in the admin panel can no longer produce a negative price.
 */
@Service
public class PricingService {

    public double unitPrice(Product product, ProductVariant variant) {
        double base = (variant != null && variant.getPriceOverride() != null)
                ? variant.getPriceOverride()
                : product.getPrice();

        Integer discount = product.getDiscountPercentage();
        int pct = discount == null ? 0 : Math.max(0, Math.min(discount, 100));

        BigDecimal basePrice = BigDecimal.valueOf(base);
        BigDecimal off = basePrice.multiply(BigDecimal.valueOf(pct))
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        return basePrice.subtract(off)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public double round2(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}