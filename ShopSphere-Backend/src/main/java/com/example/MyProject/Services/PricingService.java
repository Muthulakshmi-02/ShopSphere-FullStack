package com.example.MyProject.Services;

import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.ProductVariant;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for what a customer pays per unit.
 * Used by CartService (add / re-price) AND OrderService (cart + Buy Now),
 * so the two flows can never disagree again.
 * The formula is identical to the one that was in CartService.addProductToCart.
 */
@Service
public class PricingService {

    public double unitPrice(Product product, ProductVariant variant) {
        double base = (variant != null && variant.getPriceOverride() != null)
                ? variant.getPriceOverride()
                : product.getPrice();

        double finalPrice = base;
        Integer discount = product.getDiscountPercentage();
        if (discount != null && discount > 0) {
            finalPrice = base - (base * discount / 100.0);
        }
        return round2(finalPrice);
    }

    public double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}