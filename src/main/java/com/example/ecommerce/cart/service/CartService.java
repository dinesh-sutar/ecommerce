package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartItemResponse;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.repository.CartRepository;
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

    @Transactional
    public CartResponse addToCart(
            String email,
            AddToCartRequest request) {

        // 1. Get logged-in user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

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
                throw new IllegalArgumentException(
                        "Requested quantity exceeds available stock");
            }

            cartItem.setQuantity(newQuantity);

        } else {

            // Check stock
            if (request.getQuantity() > product.getStock()) {
                throw new IllegalArgumentException(
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

                    BigDecimal totalPrice = item.getProduct()
                            .getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getQuantity()));

                    return CartItemResponse.builder()
                            .productId(
                                    item.getProduct().getId())
                            .productName(
                                    item.getProduct().getName())
                            .sku(
                                    item.getProduct().getSku())
                            .price(
                                    item.getProduct().getPrice())
                            .quantity(
                                    item.getQuantity())
                            .totalPrice(totalPrice)
                            .build();
                })
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalAmount(totalAmount)
                .build();
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in cart"));

        Product product = cartItem.getProduct();

        if (quantity > product.getStock()) {
            throw new IllegalArgumentException(
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
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in cart"));

        cart.getItems().remove(cartItem);

        cartItemRepository.delete(cartItem);

        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        cart.getItems().clear();

        cartRepository.save(cart);
    }

}