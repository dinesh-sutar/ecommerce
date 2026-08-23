package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartItemResponse;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.coupon.dto.ApplyCouponRequest;
import com.example.ecommerce.coupon.entity.Coupon;
import com.example.ecommerce.coupon.service.CouponService;
import com.example.ecommerce.discount.dto.ProductDiscountResult;
import com.example.ecommerce.discount.service.DiscountService;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.exception.InvalidCouponException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

        private final CartRepository cartRepository;
        private final CartItemRepository cartItemRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final CouponService couponService;
        private final DiscountService discountService;

        @Transactional
        public CartResponse addToCart(
                        String email,
                        AddToCartRequest request) {

                // 1. Get logged-in user
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                // 2. Get or create cart
                Cart cart = cartRepository.findByUserId(user.getId())
                                .orElseGet(() -> {

                                        Cart newCart = Cart.builder()
                                                        .user(user)
                                                        .items(new ArrayList<>())
                                                        .build();

                                        return cartRepository.save(newCart);
                                });

                // 3. Find product
                Product product = productRepository.findById(
                                request.getProductId())
                                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                // 4. Check whether product already exists in cart
                CartItem cartItem = cartItemRepository
                                .findByCartIdAndProductId(
                                                cart.getId(),
                                                product.getId())
                                .orElse(null);

                int newQuantity;

                if (cartItem != null) {

                        newQuantity = cartItem.getQuantity()
                                        + request.getQuantity();

                        // Check stock
                        if (newQuantity > product.getStock()) {
                                throw new InsufficientStockException(
                                                "Requested quantity exceeds available stock");
                        }

                        cartItem.setQuantity(newQuantity);

                } else {

                        // Check stock
                        if (request.getQuantity() > product.getStock()) {
                                throw new InsufficientStockException(
                                                "Requested quantity exceeds available stock");
                        }

                        cartItem = CartItem.builder()
                                        .cart(cart)
                                        .product(product)
                                        .quantity(request.getQuantity())
                                        .build();

                        cart.getItems().add(cartItem);
                }

                cartItemRepository.save(cartItem);

                return buildCartResponse(cart);
        }

        private CartResponse buildCartResponse(Cart cart) {

                List<CartItemResponse> items = cart.getItems()
                                .stream()
                                .map(item -> {

                                        Product product = item.getProduct();

                                        BigDecimal subtotal = product.getPrice()
                                                        .multiply(BigDecimal.valueOf(item.getQuantity()));

                                        // Find the best applicable product-level discount
                                        ProductDiscountResult discountResult = discountService.getBestDiscount(
                                                        product.getId(),
                                                        product.getPrice(),
                                                        item.getQuantity());

                                        return CartItemResponse.builder()
                                                        .productId(product.getId())
                                                        .productName(product.getName())
                                                        .sku(product.getSku())
                                                        .price(product.getPrice())
                                                        .quantity(item.getQuantity())
                                                        .subtotal(subtotal)
                                                        .discountId(discountResult.getDiscountId())
                                                        .discountCode(discountResult.getDiscountCode())
                                                        .discountAmount(discountResult.getDiscountAmount())
                                                        .totalPrice(discountResult.getFinalAmount())
                                                        .build();
                                })
                                .toList();

                // Original cart subtotal before product-level discounts
                BigDecimal subtotal = items.stream()
                                .map(CartItemResponse::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Total discount applied across all products
                BigDecimal discountAmount = items.stream()
                                .map(CartItemResponse::getDiscountAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Subtotal after product-level discounts
                BigDecimal amountAfterProductDiscount = subtotal
                                .subtract(discountAmount);

                String couponCode = null;
                BigDecimal couponDiscount = BigDecimal.ZERO;

                // Apply coupon after product-level discounts
                if (cart.getCoupon() != null) {

                        Coupon coupon = cart.getCoupon();

                        try {
                                Coupon validCoupon = couponService.getValidCoupon(
                                                coupon.getCode(),
                                                amountAfterProductDiscount);

                                couponCode = validCoupon.getCode();

                                couponDiscount = couponService.calculateCouponDiscount(
                                                validCoupon,
                                                amountAfterProductDiscount);

                        } catch (InvalidCouponException ex) {

                                // Remove invalid or expired coupon from the cart
                                cart.setCoupon(null);
                                cartRepository.save(cart);
                        }
                }

                // Final amount after all discounts
                BigDecimal totalAmount = amountAfterProductDiscount
                                .subtract(couponDiscount);

                return CartResponse.builder()
                                .cartId(cart.getId())
                                .items(items)
                                .subtotal(subtotal)
                                .discountAmount(discountAmount)
                                .couponCode(couponCode)
                                .couponDiscount(couponDiscount)
                                .totalAmount(totalAmount)
                                .build();
        }

        @Transactional(readOnly = true)
        public CartResponse getCart(String email) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Cart cart = cartRepository.findByUserId(user.getId())
                                .orElseGet(() -> {

                                        Cart newCart = Cart.builder()
                                                        .user(user)
                                                        .items(new ArrayList<>())
                                                        .build();

                                        return cartRepository.save(newCart);
                                });

                return buildCartResponse(cart);
        }

        @Transactional
        public CartResponse updateCartItem(
                        String email,
                        Long productId,
                        Integer quantity) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Cart cart = cartRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

                CartItem cartItem = cartItemRepository
                                .findByCartIdAndProductId(cart.getId(), productId)
                                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));

                Product product = cartItem.getProduct();

                if (quantity > product.getStock()) {
                        throw new InsufficientStockException(
                                        "Requested quantity exceeds available stock");
                }

                cartItem.setQuantity(quantity);

                cartItemRepository.save(cartItem);

                return buildCartResponse(cart);
        }

        @Transactional
        public CartResponse removeCartItem(
                        String email,
                        Long productId) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Cart cart = cartRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

                CartItem cartItem = cartItemRepository
                                .findByCartIdAndProductId(cart.getId(), productId)
                                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));

                cart.getItems().remove(cartItem);

                cartItemRepository.delete(cartItem);

                return buildCartResponse(cart);
        }

        @Transactional
        public void clearCart(String email) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Cart cart = cartRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

                cart.getItems().clear();

                cartRepository.save(cart);
        }

        @Transactional
        public CartResponse applyCoupon(
                        String email,
                        ApplyCouponRequest request) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Cart cart = cartRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

                if (cart.getItems().isEmpty()) {
                        throw new InvalidCouponException(
                                        "Cannot apply coupon to an empty cart");
                }

                // Calculate cart amount after product discounts
                BigDecimal discountedCartAmount = BigDecimal.ZERO;

                for (CartItem item : cart.getItems()) {

                        Product product = item.getProduct();

                        ProductDiscountResult discountResult = discountService.getBestDiscount(
                                        product.getId(),
                                        product.getPrice(),
                                        item.getQuantity());

                        discountedCartAmount = discountedCartAmount
                                        .add(discountResult.getFinalAmount());
                }

                // Validate coupon against discounted cart amount
                Coupon coupon = couponService.getValidCoupon(
                                request.getCode(),
                                discountedCartAmount);

                // Attach coupon to cart
                cart.setCoupon(coupon);

                cartRepository.save(cart);

                return buildCartResponse(cart);
        }

        @Transactional
        public CartResponse removeCoupon(String email) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Cart cart = cartRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

                if (cart.getCoupon() == null) {
                        throw new InvalidCouponException(
                                        "No coupon is applied to this cart");
                }

                cart.setCoupon(null);

                cartRepository.save(cart);

                return buildCartResponse(cart);
        }

}