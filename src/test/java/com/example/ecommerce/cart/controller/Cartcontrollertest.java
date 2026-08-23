package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartItemResponse;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.coupon.dto.ApplyCouponRequest;
import com.example.ecommerce.exception.InvalidCouponException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@WithMockUser(username = "test@example.com")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtService jwtService;

    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {

        CartItemResponse item = CartItemResponse.builder()
                .productId(1L)
                .productName("Wireless Mouse")
                .sku("WM-001")
                .price(BigDecimal.valueOf(799))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(1598))
                .totalPrice(BigDecimal.valueOf(1598))
                .build();

        cartResponse = CartResponse.builder()
                .cartId(1L)
                .items(List.of(item))
                .subtotal(BigDecimal.valueOf(1598))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(1598))
                .build();
    }

    @Test
    void addToCart_withValidRequest_returns200() throws Exception {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartService.addToCart(anyString(), any()))
                .thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/items")
                .with(user("test@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("Wireless Mouse"));
    }

    @Test
    void addToCart_withInvalidQuantity_returns400() throws Exception {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(0);

        mockMvc.perform(post("/api/cart/items")
                .with(user("test@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addToCart_productNotFound_returns404() throws Exception {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(999L);
        request.setQuantity(1);

        when(cartService.addToCart(anyString(), any()))
                .thenThrow(
                        new ResourceNotFoundException("Product not found"));

        mockMvc.perform(post("/api/cart/items")
                .with(user("test@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCart_returnsCart() throws Exception {

        when(cartService.getCart(anyString()))
                .thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart")
                .with(user("test@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1598));
    }

    @Test
    void updateCartItem_withValidQuantity_returns200() throws Exception {

        UpdateCartItemRequest request = new UpdateCartItemRequest();

        request.setQuantity(5);

        when(cartService.updateCartItem(
                anyString(),
                eq(1L),
                eq(5))).thenReturn(cartResponse);

        mockMvc.perform(put("/api/cart/items/1")
                .with(user("test@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void removeCartItem_returns200() throws Exception {

        when(cartService.removeCartItem(
                anyString(),
                eq(1L))).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/items/1")
                .with(user("test@example.com"))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void clearCart_returns204() throws Exception {

        mockMvc.perform(delete("/api/cart")
                .with(user("test@example.com"))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void applyCoupon_withValidCode_returns200() throws Exception {

        ApplyCouponRequest request = new ApplyCouponRequest();

        request.setCode("SAVE10");

        when(cartService.applyCoupon(
                anyString(),
                any())).thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/coupon")
                .with(user("test@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void applyCoupon_withInvalidCode_returns400() throws Exception {

        ApplyCouponRequest request = new ApplyCouponRequest();

        request.setCode("EXPIRED10");

        when(cartService.applyCoupon(
                anyString(),
                any())).thenThrow(
                        new InvalidCouponException("Coupon has expired"));

        mockMvc.perform(post("/api/cart/coupon")
                .with(user("test@example.com"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Coupon has expired"));
    }

    @Test
    void removeCoupon_returns200() throws Exception {

        when(cartService.removeCoupon(anyString()))
                .thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/coupon")
                .with(user("test@example.com"))
                .with(csrf()))
                .andExpect(status().isOk());
    }
}