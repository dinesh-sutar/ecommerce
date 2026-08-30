package com.example.ecommerce.coupon.controller;

import com.example.ecommerce.coupon.dto.CouponResponse;
import com.example.ecommerce.coupon.dto.CreateCouponRequest;
import com.example.ecommerce.coupon.service.CouponService;
import com.example.ecommerce.discount.enums.DiscountType;
import com.example.ecommerce.security.JwtService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CouponController.class)
@AutoConfigureMockMvc(addFilters = false)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponService couponService;

    /*
     * Required because JwtAuthenticationFilter depends on JwtService.
     * WebMvcTest does not load the real JwtService bean.
     */
    @MockBean
    private JwtService jwtService;

    private CouponResponse couponResponse;

    private CreateCouponRequest request;

    @BeforeEach
    void setUp() {

        couponResponse = CouponResponse.builder()
                .id(1L)
                .code("SAVE10")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .minimumCartValue(BigDecimal.valueOf(500))
                .maximumDiscount(BigDecimal.valueOf(200))
                .expiryDate(LocalDateTime.of(2026, 12, 31, 23, 59))
                .active(true)
                .build();

        request = CreateCouponRequest.builder()
                .code("SAVE10")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .minimumCartValue(BigDecimal.valueOf(500))
                .maximumDiscount(BigDecimal.valueOf(200))
                .expiryDate(LocalDateTime.of(2026, 12, 31, 23, 59))
                .active(true)
                .build();
    }

    // =========================================================
    // CREATE COUPON - SUCCESS
    // =========================================================

    @Test
    void createCoupon_withValidRequest_returns201()
            throws Exception {

        when(couponService.createCoupon(any(CreateCouponRequest.class)))
                .thenReturn(couponResponse);

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE"))
                .andExpect(jsonPath("$.value").value(10))
                .andExpect(jsonPath("$.minimumCartValue").value(500))
                .andExpect(jsonPath("$.maximumDiscount").value(200))
                .andExpect(jsonPath("$.active").value(true));

        verify(couponService)
                .createCoupon(any(CreateCouponRequest.class));
    }

    // =========================================================
    // CREATE COUPON - VALIDATION
    // =========================================================

    @Test
    void createCoupon_withBlankCode_returns400()
            throws Exception {

        request.setCode("");

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCoupon_withMissingType_returns400()
            throws Exception {

        request.setType(null);

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCoupon_withMissingValue_returns400()
            throws Exception {

        request.setValue(null);

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCoupon_withZeroValue_returns400()
            throws Exception {

        request.setValue(BigDecimal.ZERO);

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCoupon_withNegativeMinimumCartValue_returns400()
            throws Exception {

        request.setMinimumCartValue(BigDecimal.valueOf(-1));

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCoupon_withZeroMaximumDiscount_returns400()
            throws Exception {

        request.setMaximumDiscount(BigDecimal.ZERO);

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCoupon_withMissingExpiryDate_returns400()
            throws Exception {

        request.setExpiryDate(null);

        mockMvc.perform(
                post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}