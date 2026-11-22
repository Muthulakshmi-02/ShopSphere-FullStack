package com.example.MyProject.Services;

import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.ProductVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
    }

    @Test
    @DisplayName("Should return product base price when variant is null and no discount")
    void testUnitPriceWithoutVariantAndNoDiscount() {
        Product product = Product.builder()
                .price(100.0)
                .discountPercentage(0)
                .build();

        double unitPrice = pricingService.unitPrice(product, null);
        assertEquals(100.0, unitPrice, 0.001);
    }

    @Test
    @DisplayName("Should apply discount percentage correctly on base price")
    void testUnitPriceWithDiscount() {
        Product product = Product.builder()
                .price(250.0)
                .discountPercentage(20) // 20% off 250 = 200
                .build();

        double unitPrice = pricingService.unitPrice(product, null);
        assertEquals(200.0, unitPrice, 0.001);
    }

    @Test
    @DisplayName("Should use variant price override when available")
    void testUnitPriceWithVariantOverride() {
        Product product = Product.builder()
                .price(100.0)
                .discountPercentage(10)
                .build();

        ProductVariant variant = ProductVariant.builder()
                .priceOverride(150.0) // 10% off 150 = 135
                .build();

        double unitPrice = pricingService.unitPrice(product, variant);
        assertEquals(135.0, unitPrice, 0.001);
    }

    @Test
    @DisplayName("Should fall back to product price if variant has no price override")
    void testUnitPriceWithVariantNoOverride() {
        Product product = Product.builder()
                .price(80.0)
                .discountPercentage(25) // 25% off 80 = 60
                .build();

        ProductVariant variant = ProductVariant.builder()
                .priceOverride(null)
                .build();

        double unitPrice = pricingService.unitPrice(product, variant);
        assertEquals(60.0, unitPrice, 0.001);
    }

    @Test
    @DisplayName("Should round to 2 decimal places properly")
    void testRound2() {
        assertEquals(12.35, pricingService.round2(12.346), 0.001);
        assertEquals(12.34, pricingService.round2(12.344), 0.001);
    }
}
