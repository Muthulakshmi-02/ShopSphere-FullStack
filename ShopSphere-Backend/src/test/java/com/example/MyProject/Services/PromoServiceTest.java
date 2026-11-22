package com.example.MyProject.Services;

import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Models.Coupon;
import com.example.MyProject.Repository.CouponRepository;
import com.example.MyProject.Repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Spy
    private PricingService pricingService = new PricingService();

    @InjectMocks
    private PromoService promoService;

    private Coupon validPercentageCoupon;

    @BeforeEach
    void setUp() {
        validPercentageCoupon = Coupon.builder()
                .code("SAVE20")
                .discountPercentage(20)
                .active(true)
                .validUntil(LocalDateTime.now().plusDays(10))
                .minOrderAmount(500.0)
                .build();
    }

    @Test
    @DisplayName("Should return null if promo code is null or empty")
    void testResolveWithBlankCode() {
        assertNull(promoService.resolve(null, "user@example.com", 1000.0));
        assertNull(promoService.resolve("   ", "user@example.com", 1000.0));
    }

    @Test
    @DisplayName("Should throw exception if user has already used the coupon")
    void testResolveAlreadyUsedCoupon() {
        when(userCouponRepository.existsByUserEmailAndCouponCode("user@example.com", "SAVE20"))
                .thenReturn(true);

        CartBusinessException exception = assertThrows(CartBusinessException.class, () ->
                promoService.resolve("SAVE20", "user@example.com", 1000.0));

        assertTrue(exception.getMessage().contains("already used this promo code"));
    }

    @Test
    @DisplayName("Should calculate percentage discount accurately for eligible subtotal")
    void testResolveValidPercentageCoupon() {
        when(userCouponRepository.existsByUserEmailAndCouponCode("user@example.com", "SAVE20"))
                .thenReturn(false);
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        PromoService.PromoResult result = promoService.resolve("SAVE20", "user@example.com", 1000.0);

        assertNotNull(result);
        assertEquals("SAVE20", result.code());
        assertEquals(200.0, result.discount(), 0.001); // 20% of 1000
    }

    @Test
    @DisplayName("Should throw exception if order subtotal does not meet minimum order amount")
    void testResolveBelowMinimumOrderAmount() {
        when(userCouponRepository.existsByUserEmailAndCouponCode("user@example.com", "SAVE20"))
                .thenReturn(false);
        when(couponRepository.findByCodeIgnoreCaseAndActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CartBusinessException exception = assertThrows(CartBusinessException.class, () ->
                promoService.resolve("SAVE20", "user@example.com", 300.0));

        assertTrue(exception.getMessage().contains("minimum"));
    }
}
