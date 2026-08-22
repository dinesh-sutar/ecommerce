package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.dto.CouponResponse;
import com.example.ecommerce.coupon.dto.CreateCouponRequest;
import com.example.ecommerce.coupon.entity.Coupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.discount.enums.DiscountType;
import com.example.ecommerce.exception.CouponAlreadyExistsException;
import com.example.ecommerce.exception.InvalidCouponException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {

        String code = request.getCode().trim().toUpperCase();

        if (couponRepository.existsByCode(code)) {
            throw new CouponAlreadyExistsException(
                    "Coupon code already exists: " + code);
        }

        if (request.getType() == DiscountType.PERCENTAGE
                && request.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new InvalidCouponException(
                    "Percentage coupon cannot be greater than 100");
        }

        if (request.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCouponException(
                    "Expiry date must be in the future");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .type(request.getType())
                .value(request.getValue())
                .minimumCartValue(request.getMinimumCartValue())
                .maximumDiscount(request.getMaximumDiscount())
                .expiryDate(request.getExpiryDate())
                .active(
                        request.getActive() != null
                                ? request.getActive()
                                : true)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        return mapToResponse(savedCoupon);
    }

    public Coupon getValidCoupon(
            String code,
            BigDecimal cartAmount) {

        String normalizedCode = code.trim().toUpperCase();

        Coupon coupon = couponRepository
                .findByCode(normalizedCode)
                .orElseThrow(() -> new InvalidCouponException("Invalid coupon code"));

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new InvalidCouponException(
                    "Coupon is inactive");
        }

        if (LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
            throw new InvalidCouponException(
                    "Coupon has expired");
        }

        if (coupon.getMinimumCartValue() != null
                && cartAmount.compareTo(
                        coupon.getMinimumCartValue()) < 0) {

            throw new InvalidCouponException(
                    "Minimum cart value of "
                            + coupon.getMinimumCartValue()
                            + " is required for this coupon");
        }

        return coupon;
    }

    public BigDecimal calculateCouponDiscount(
            Coupon coupon,
            BigDecimal cartAmount) {

        BigDecimal discountAmount;

        if (coupon.getType() == DiscountType.PERCENTAGE) {

            discountAmount = cartAmount
                    .multiply(coupon.getValue())
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP);

        } else {

            discountAmount = coupon.getValue();
        }

        if (coupon.getMaximumDiscount() != null
                && discountAmount.compareTo(
                        coupon.getMaximumDiscount()) > 0) {

            discountAmount = coupon.getMaximumDiscount();
        }

        // Coupon discount should never exceed the cart amount
        if (discountAmount.compareTo(cartAmount) > 0) {
            discountAmount = cartAmount;
        }

        return discountAmount;
    }

    private CouponResponse mapToResponse(Coupon coupon) {

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minimumCartValue(coupon.getMinimumCartValue())
                .maximumDiscount(coupon.getMaximumDiscount())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.getActive())
                .build();
    }
}