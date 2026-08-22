package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.coupon.dto.ApplyCouponRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

        private final CartService cartService;

        // Add product to cart
        @PostMapping("/items")
        @ResponseStatus(HttpStatus.OK)
        public CartResponse addToCart(
                        Authentication authentication,
                        @Valid @RequestBody AddToCartRequest request) {

                return cartService.addToCart(
                                authentication.getName(),
                                request);
        }

        // Get logged-in user's cart
        @GetMapping
        public CartResponse getCart(
                        Authentication authentication) {

                return cartService.getCart(
                                authentication.getName());
        }

        // Update product quantity
        @PutMapping("/items/{productId}")
        public CartResponse updateCartItem(
                        Authentication authentication,
                        @PathVariable Long productId,
                        @Valid @RequestBody UpdateCartItemRequest request) {

                return cartService.updateCartItem(
                                authentication.getName(),
                                productId,
                                request.getQuantity());
        }

        // Remove one product from cart
        @DeleteMapping("/items/{productId}")
        public CartResponse removeCartItem(
                        Authentication authentication,
                        @PathVariable Long productId) {

                return cartService.removeCartItem(
                                authentication.getName(),
                                productId);
        }

        // Clear entire cart
        @DeleteMapping
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void clearCart(
                        Authentication authentication) {

                cartService.clearCart(
                                authentication.getName());
        }

        // Apply coupon to cart
        @PostMapping("/coupon")
        public CartResponse applyCoupon(
                        Authentication authentication,
                        @Valid @RequestBody ApplyCouponRequest request) {

                return cartService.applyCoupon(
                                authentication.getName(),
                                request);
        }

        // Remove applied coupon from cart
        @DeleteMapping("/coupon")
        public CartResponse removeCoupon(
                        Authentication authentication) {

                return cartService.removeCoupon(
                                authentication.getName());
        }
}