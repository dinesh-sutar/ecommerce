package com.example.ecommerce.coupon.service;

import com.example.ecommerce.coupon.dto.CouponResponse;
import com.example.ecommerce.coupon.dto.CreateCouponRequest;
import com.example.ecommerce.coupon.entity.Coupon;
import com.example.ecommerce.coupon.repository.CouponRepository;
import com.example.ecommerce.discount.enums.DiscountType;
import com.example.ecommerce.exception.CouponAlreadyExistsException;
import com.example.ecommerce.exception.InvalidCouponException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private CreateCouponRequest validRequest;
    private Coupon validCoupon;

    @BeforeEach
    void setUp() {

        validRequest = CreateCouponRequest.builder()
                .code("save10")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .minimumCartValue(BigDecimal.valueOf(100))
                .maximumDiscount(BigDecimal.valueOf(50))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .active(true)
                .build();

        validCoupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .minimumCartValue(BigDecimal.valueOf(100))
                .maximumDiscount(BigDecimal.valueOf(50))
                .expiryDate(LocalDateTime.now().plusDays(7))
                .active(true)
                .build();
    }

    // =========================================================
    // CREATE COUPON TESTS
    // =========================================================

    @Test
    void createCoupon_ShouldCreateCouponSuccessfully() {

        when(couponRepository.existsByCode("SAVE10"))
                .thenReturn(false);

        when(couponRepository.save(any(Coupon.class)))
                .thenReturn(validCoupon);

        CouponResponse response = couponService.createCoupon(validRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("SAVE10", response.getCode());
        assertEquals(DiscountType.PERCENTAGE, response.getType());
        assertEquals(BigDecimal.valueOf(10), response.getValue());
        assertEquals(BigDecimal.valueOf(100),
                response.getMinimumCartValue());
        assertEquals(BigDecimal.valueOf(50),
                response.getMaximumDiscount());
        assertTrue(response.getActive());

        ArgumentCaptor<Coupon> couponCaptor = ArgumentCaptor.forClass(Coupon.class);

        verify(couponRepository)
                .save(couponCaptor.capture());

        Coupon savedCoupon = couponCaptor.getValue();

        assertEquals("SAVE10", savedCoupon.getCode());
        assertEquals(DiscountType.PERCENTAGE,
                savedCoupon.getType());
        assertEquals(BigDecimal.valueOf(10),
                savedCoupon.getValue());
    }

    @Test
    void createCoupon_ShouldNormalizeCouponCode() {

        when(couponRepository.existsByCode("SAVE10"))
                .thenReturn(false);

        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> {

                    Coupon coupon = invocation.getArgument(0);
                    coupon.setId(1L);

                    return coupon;
                });

        validRequest.setCode("  save10  ");

        CouponResponse response = couponService.createCoupon(validRequest);

        assertEquals("SAVE10", response.getCode());

        verify(couponRepository)
                .existsByCode("SAVE10");
    }

    @Test
    void createCoupon_ShouldThrowException_WhenCouponAlreadyExists() {

        when(couponRepository.existsByCode("SAVE10"))
                .thenReturn(true);

        CouponAlreadyExistsException exception = assertThrows(
                CouponAlreadyExistsException.class,
                () -> couponService.createCoupon(validRequest));

        assertEquals(
                "Coupon code already exists: SAVE10",
                exception.getMessage());

        verify(couponRepository, never())
                .save(any());
    }

    @Test
    void createCoupon_ShouldThrowException_WhenPercentageGreaterThan100() {

        validRequest.setType(DiscountType.PERCENTAGE);
        validRequest.setValue(BigDecimal.valueOf(101));

        when(couponRepository.existsByCode("SAVE10"))
                .thenReturn(false);

        InvalidCouponException exception = assertThrows(
                InvalidCouponException.class,
                () -> couponService.createCoupon(validRequest));

        assertEquals(
                "Percentage coupon cannot be greater than 100",
                exception.getMessage());

        verify(couponRepository, never())
                .save(any());
    }

    @Test
    void createCoupon_ShouldThrowException_WhenExpiryDateIsInPast() {

        validRequest.setExpiryDate(
                LocalDateTime.now().minusDays(1));

        when(couponRepository.existsByCode("SAVE10"))
                .thenReturn(false);

        InvalidCouponException exception = assertThrows(
                InvalidCouponException.class,
                () -> couponService.createCoupon(validRequest));

        assertEquals(
                "Expiry date must be in the future",
                exception.getMessage());

        verify(couponRepository, never())
                .save(any());
    }

    @Test
    void createCoupon_ShouldDefaultActiveToTrue_WhenActiveIsNull() {

        validRequest.setActive(null);

        when(couponRepository.existsByCode("SAVE10"))
                .thenReturn(false);

        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> {

                    Coupon coupon = invocation.getArgument(0);
                    coupon.setId(1L);

                    return coupon;
                });

        CouponResponse response = couponService.createCoupon(validRequest);

        assertTrue(response.getActive());

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);

        verify(couponRepository)
                .save(captor.capture());

        assertTrue(captor.getValue().getActive());
    }

    // =========================================================
    // GET VALID COUPON TESTS
    // =========================================================

    @Test
    void getValidCoupon_ShouldReturnCoupon_WhenCouponIsValid() {

        when(couponRepository.findByCode("SAVE10"))
                .thenReturn(Optional.of(validCoupon));

        Coupon result = couponService.getValidCoupon(
                "save10",
                BigDecimal.valueOf(200));

        assertNotNull(result);
        assertEquals("SAVE10", result.getCode());

        verify(couponRepository)
                .findByCode("SAVE10");
    }

    @Test
    void getValidCoupon_ShouldNormalizeCouponCode() {

        when(couponRepository.findByCode("SAVE10"))
                .thenReturn(Optional.of(validCoupon));

        couponService.getValidCoupon(
                "  save10  ",
                BigDecimal.valueOf(200));

        verify(couponRepository)
                .findByCode("SAVE10");
    }

    @Test
    void getValidCoupon_ShouldThrowException_WhenCouponNotFound() {

        when(couponRepository.findByCode("INVALID"))
                .thenReturn(Optional.empty());

        InvalidCouponException exception = assertThrows(
                InvalidCouponException.class,
                () -> couponService.getValidCoupon(
                        "invalid",
                        BigDecimal.valueOf(200)));

        assertEquals(
                "Invalid coupon code",
                exception.getMessage());
    }

    @Test
    void getValidCoupon_ShouldThrowException_WhenCouponIsInactive() {

        validCoupon.setActive(false);

        when(couponRepository.findByCode("SAVE10"))
                .thenReturn(Optional.of(validCoupon));

        InvalidCouponException exception = assertThrows(
                InvalidCouponException.class,
                () -> couponService.getValidCoupon(
                        "SAVE10",
                        BigDecimal.valueOf(200)));

        assertEquals(
                "Coupon is inactive",
                exception.getMessage());
    }

    @Test
    void getValidCoupon_ShouldThrowException_WhenCouponExpired() {

        validCoupon.setExpiryDate(
                LocalDateTime.now().minusDays(1));

        when(couponRepository.findByCode("SAVE10"))
                .thenReturn(Optional.of(validCoupon));

        InvalidCouponException exception = assertThrows(
                InvalidCouponException.class,
                () -> couponService.getValidCoupon(
                        "SAVE10",
                        BigDecimal.valueOf(200)));

        assertEquals(
                "Coupon has expired",
                exception.getMessage());
    }

    @Test
    void getValidCoupon_ShouldThrowException_WhenMinimumCartValueNotReached() {

        validCoupon.setMinimumCartValue(
                BigDecimal.valueOf(500));

        when(couponRepository.findByCode("SAVE10"))
                .thenReturn(Optional.of(validCoupon));

        InvalidCouponException exception = assertThrows(
                InvalidCouponException.class,
                () -> couponService.getValidCoupon(
                        "SAVE10",
                        BigDecimal.valueOf(200)));

        assertEquals(
                "Minimum cart value of 500 is required for this coupon",
                exception.getMessage());
    }

    @Test
    void getValidCoupon_ShouldAllowCoupon_WhenMinimumCartValueIsExactlyMet() {

        validCoupon.setMinimumCartValue(
                BigDecimal.valueOf(200));

        when(couponRepository.findByCode("SAVE10"))
                .thenReturn(Optional.of(validCoupon));

        Coupon result = couponService.getValidCoupon(
                "SAVE10",
                BigDecimal.valueOf(200));

        assertNotNull(result);
        assertEquals("SAVE10", result.getCode());
    }

    @Test
    void getValidCoupon_ShouldAllowCoupon_WhenMinimumCartValueIsNull() {

        validCoupon.setMinimumCartValue(null);

        when(couponRepository.findByCode("SAVE10"))
                .thenReturn(Optional.of(validCoupon));

        Coupon result = couponService.getValidCoupon(
                "SAVE10",
                BigDecimal.valueOf(10));

        assertNotNull(result);
    }

    // =========================================================
    // CALCULATE COUPON DISCOUNT TESTS
    // =========================================================

    @Test
    void calculateCouponDiscount_ShouldCalculatePercentageDiscount() {

        validCoupon.setType(DiscountType.PERCENTAGE);
        validCoupon.setValue(BigDecimal.valueOf(10));
        validCoupon.setMaximumDiscount(null);

        BigDecimal result = couponService.calculateCouponDiscount(
                validCoupon,
                BigDecimal.valueOf(1000));

        assertEquals(
                BigDecimal.valueOf(100).setScale(2),
                result);
    }

    @Test
    void calculateCouponDiscount_ShouldCalculatePercentageWithRounding() {

        validCoupon.setType(DiscountType.PERCENTAGE);
        validCoupon.setValue(BigDecimal.valueOf(15));
        validCoupon.setMaximumDiscount(null);

        BigDecimal result = couponService.calculateCouponDiscount(
                validCoupon,
                BigDecimal.valueOf(99));

        assertEquals(
                BigDecimal.valueOf(14.85).setScale(2),
                result);
    }

    @Test
    void calculateCouponDiscount_ShouldCalculateFlatDiscount() {

        validCoupon.setType(DiscountType.FLAT);
        validCoupon.setValue(BigDecimal.valueOf(100));
        validCoupon.setMaximumDiscount(null);

        BigDecimal result = couponService.calculateCouponDiscount(
                validCoupon,
                BigDecimal.valueOf(500));

        assertEquals(
                BigDecimal.valueOf(100),
                result);
    }

    @Test
    void calculateCouponDiscount_ShouldApplyMaximumDiscountCap() {

        validCoupon.setType(DiscountType.PERCENTAGE);
        validCoupon.setValue(BigDecimal.valueOf(50));
        validCoupon.setMaximumDiscount(
                BigDecimal.valueOf(100));

        BigDecimal result = couponService.calculateCouponDiscount(
                validCoupon,
                BigDecimal.valueOf(1000));

        assertEquals(
                BigDecimal.valueOf(100),
                result);
    }

    @Test
    void calculateCouponDiscount_ShouldNotExceedCartAmount_ForFlatDiscount() {

        validCoupon.setType(DiscountType.FLAT);
        validCoupon.setValue(BigDecimal.valueOf(500));
        validCoupon.setMaximumDiscount(null);

        BigDecimal result = couponService.calculateCouponDiscount(
                validCoupon,
                BigDecimal.valueOf(200));

        assertEquals(
                BigDecimal.valueOf(200),
                result);
    }

    @Test
    void calculateCouponDiscount_ShouldNotExceedCartAmount_AfterMaximumCap() {

        validCoupon.setType(DiscountType.PERCENTAGE);
        validCoupon.setValue(BigDecimal.valueOf(100));
        validCoupon.setMaximumDiscount(
                BigDecimal.valueOf(500));

        BigDecimal result = couponService.calculateCouponDiscount(
                validCoupon,
                BigDecimal.valueOf(200));

        assertEquals(0, BigDecimal.valueOf(200).compareTo(result));
    }
}