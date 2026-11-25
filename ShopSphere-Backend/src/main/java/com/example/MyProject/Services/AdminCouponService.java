package com.example.MyProject.Services;

import com.example.MyProject.Coupon.dto.CouponRequest;
import com.example.MyProject.Coupon.dto.CouponResponse;
import com.example.MyProject.Exception.CartBusinessException;
import com.example.MyProject.Models.Coupon;
import com.example.MyProject.Repository.CouponRepository;
import com.example.MyProject.Repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CartBusinessException("Coupon not found with id: " + id));
        return toResponse(coupon);
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest dto) {
        String code = dto.getCode().trim().toUpperCase(Locale.ROOT);
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new CartBusinessException("A coupon with code '" + code + "' already exists.");
        }

        String type = dto.getType().trim().toLowerCase(Locale.ROOT);
        double value = dto.getValue();

        if ("percent".equals(type)) {
            if (value > 100) {
                throw new CartBusinessException("Percentage discount cannot exceed 100%.");
            }
            if (value >= 1) {
                value = value / 100.0;
            }
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .type(type)
                .value(value)
                .active(dto.isActive())
                .minOrderAmount(dto.getMinOrderAmount())
                .expiresAt(dto.getExpiresAt())
                .build();

        Coupon saved = couponRepository.save(coupon);
        return toResponse(saved);
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest dto) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CartBusinessException("Coupon not found with id: " + id));

        String code = dto.getCode().trim().toUpperCase(Locale.ROOT);
        if (couponRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new CartBusinessException("Another coupon with code '" + code + "' already exists.");
        }

        String type = dto.getType().trim().toLowerCase(Locale.ROOT);
        double value = dto.getValue();

        if ("percent".equals(type)) {
            if (value > 100) {
                throw new CartBusinessException("Percentage discount cannot exceed 100%.");
            }
            if (value >= 1) {
                value = value / 100.0;
            }
        }

        coupon.setCode(code);
        coupon.setType(type);
        coupon.setValue(value);
        coupon.setActive(dto.isActive());
        coupon.setMinOrderAmount(dto.getMinOrderAmount());
        coupon.setExpiresAt(dto.getExpiresAt());

        Coupon updated = couponRepository.save(coupon);
        return toResponse(updated);
    }

    @Transactional
    public CouponResponse toggleCouponActive(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CartBusinessException("Coupon not found with id: " + id));

        coupon.setActive(!coupon.isActive());
        Coupon updated = couponRepository.save(coupon);
        return toResponse(updated);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new CartBusinessException("Coupon not found with id: " + id);
        }
        couponRepository.deleteById(id);
    }

    private CouponResponse toResponse(Coupon coupon) {
        boolean expired = coupon.getExpiresAt() != null && LocalDateTime.now().isAfter(coupon.getExpiresAt());
        long usageCount = userCouponRepository.countByCouponCodeIgnoreCase(coupon.getCode());

        boolean isPercent = "percent".equalsIgnoreCase(coupon.getType());
        double displayValue = isPercent
                ? (coupon.getValue() < 1 ? coupon.getValue() * 100.0 : coupon.getValue())
                : coupon.getValue();

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .type(coupon.getType())
                .value(coupon.getValue())
                .displayValue(Math.round(displayValue * 100.0) / 100.0)
                .active(coupon.isActive())
                .minOrderAmount(coupon.getMinOrderAmount())
                .expiresAt(coupon.getExpiresAt())
                .expired(expired)
                .usageCount(usageCount)
                .build();
    }
}
