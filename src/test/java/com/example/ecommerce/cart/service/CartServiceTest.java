package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.AddToCartRequest;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("TEST-001")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())
                .build();

        cartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(2)
                .build();

        cart.getItems().add(cartItem);
    }

    private ProductDiscountResult noDiscount(
            Product product,
            int quantity) {

        BigDecimal subtotal = product.getPrice()
                .multiply(BigDecimal.valueOf(quantity));

        return ProductDiscountResult.builder()
                .discountId(null)
                .discountCode(null)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(subtotal)
                .build();
    }

    // =========================================================
    // ADD TO CART
    // =========================================================

    @Test
    void addToCart_shouldAddProductToNewCart() {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        Cart newCart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())
                .build();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class)))
                .thenReturn(newCart);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        when(discountService.getBestDiscount(
                eq(1L),
                eq(product.getPrice()),
                eq(2)))
                .thenReturn(noDiscount(product, 2));

        CartResponse response = cartService.addToCart("test@example.com", request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("200.00"), response.getSubtotal());
        assertEquals(BigDecimal.ZERO, response.getDiscountAmount());
        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());

        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addToCart_shouldIncreaseQuantityWhenProductAlreadyExists() {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(cartItem));

        when(discountService.getBestDiscount(
                eq(1L),
                eq(product.getPrice()),
                eq(5)))
                .thenReturn(noDiscount(product, 5));

        CartResponse response = cartService.addToCart("test@example.com", request);

        assertEquals(5, cartItem.getQuantity());
        assertEquals(new BigDecimal("500.00"), response.getTotalAmount());

        verify(cartItemRepository).save(cartItem);
    }

    @Test
    void addToCart_shouldThrowExceptionWhenUserNotFound() {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addToCart(
                        "test@example.com",
                        request));

        verifyNoInteractions(
                cartRepository,
                productRepository,
                cartItemRepository);
    }

    @Test
    void addToCart_shouldThrowExceptionWhenProductNotFound() {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addToCart(
                        "test@example.com",
                        request));
    }

    @Test
    void addToCart_shouldThrowExceptionWhenNewQuantityExceedsStock() {

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(20);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addToCart(
                        "test@example.com",
                        request));

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addToCart_shouldThrowExceptionWhenExistingQuantityExceedsStock() {

        cartItem.setQuantity(8);

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(5);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addToCart(
                        "test@example.com",
                        request));

        verify(cartItemRepository, never()).save(any());
    }

    // =========================================================
    // GET CART
    // =========================================================

    @Test
    void getCart_shouldReturnExistingCart() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(discountService.getBestDiscount(
                eq(1L),
                eq(product.getPrice()),
                eq(2)))
                .thenReturn(noDiscount(product, 2));

        CartResponse response = cartService.getCart("test@example.com");

        assertNotNull(response);
        assertEquals(1L, response.getCartId());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());
    }

    @Test
    void getCart_shouldThrowExceptionWhenUserNotFound() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.getCart("test@example.com"));
    }

    // =========================================================
    // UPDATE CART ITEM
    // =========================================================

    @Test
    void updateCartItem_shouldUpdateQuantity() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(cartItem));

        when(discountService.getBestDiscount(
                eq(1L),
                eq(product.getPrice()),
                eq(5)))
                .thenReturn(noDiscount(product, 5));

        CartResponse response = cartService.updateCartItem(
                "test@example.com",
                1L,
                5);

        assertEquals(5, cartItem.getQuantity());
        assertEquals(new BigDecimal("500.00"), response.getTotalAmount());

        verify(cartItemRepository).save(cartItem);
    }

    @Test
    void updateCartItem_shouldThrowExceptionWhenCartNotFound() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateCartItem(
                        "test@example.com",
                        1L,
                        5));
    }

    @Test
    void updateCartItem_shouldThrowExceptionWhenProductNotInCart() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateCartItem(
                        "test@example.com",
                        1L,
                        5));
    }

    @Test
    void updateCartItem_shouldThrowExceptionWhenQuantityExceedsStock() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.updateCartItem(
                        "test@example.com",
                        1L,
                        20));

        verify(cartItemRepository, never()).save(any());
    }

    // =========================================================
    // REMOVE CART ITEM
    // =========================================================

    @Test
    void removeCartItem_shouldRemoveProductFromCart() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(cartItem));

        CartResponse response = cartService.removeCartItem(
                "test@example.com",
                1L);

        assertTrue(cart.getItems().isEmpty());
        assertTrue(response.getItems().isEmpty());

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    void removeCartItem_shouldThrowExceptionWhenCartNotFound() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.removeCartItem(
                        "test@example.com",
                        1L));
    }

    @Test
    void removeCartItem_shouldThrowExceptionWhenProductNotFoundInCart() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.removeCartItem(
                        "test@example.com",
                        1L));
    }

    // =========================================================
    // CLEAR CART
    // =========================================================

    @Test
    void clearCart_shouldRemoveAllItems() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        cartService.clearCart("test@example.com");

        assertTrue(cart.getItems().isEmpty());

        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_shouldThrowExceptionWhenCartNotFound() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.clearCart("test@example.com"));
    }

    // =========================================================
    // APPLY COUPON
    // =========================================================

    @Test
    void applyCoupon_shouldApplyCouponSuccessfully() {

        ApplyCouponRequest request = new ApplyCouponRequest();

        request.setCode("SAVE10");

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .build();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(discountService.getBestDiscount(
                eq(1L),
                eq(product.getPrice()),
                eq(2)))
                .thenReturn(noDiscount(product, 2));

        when(couponService.getValidCoupon(
                "SAVE10",
                new BigDecimal("200.00")))
                .thenReturn(coupon);

        when(couponService.calculateCouponDiscount(
                coupon,
                new BigDecimal("200.00")))
                .thenReturn(new BigDecimal("20.00"));

        CartResponse response = cartService.applyCoupon(
                "test@example.com",
                request);

        assertEquals(coupon, cart.getCoupon());
        assertEquals("SAVE10", response.getCouponCode());
        assertEquals(
                new BigDecimal("20.00"),
                response.getCouponDiscount());

        assertEquals(
                new BigDecimal("180.00"),
                response.getTotalAmount());

        verify(cartRepository).save(cart);
    }

    @Test
    void applyCoupon_shouldThrowExceptionWhenCartIsEmpty() {

        cart.getItems().clear();

        ApplyCouponRequest request = new ApplyCouponRequest();

        request.setCode("SAVE10");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        assertThrows(
                InvalidCouponException.class,
                () -> cartService.applyCoupon(
                        "test@example.com",
                        request));

        verifyNoInteractions(couponService);
    }

    // =========================================================
    // REMOVE COUPON
    // =========================================================

    @Test
    void removeCoupon_shouldRemoveCouponSuccessfully() {

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("SAVE10")
                .build();

        cart.setCoupon(coupon);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(discountService.getBestDiscount(
                eq(1L),
                eq(product.getPrice()),
                eq(2)))
                .thenReturn(noDiscount(product, 2));

        CartResponse response = cartService.removeCoupon("test@example.com");

        assertNull(cart.getCoupon());
        assertNull(response.getCouponCode());

        verify(cartRepository).save(cart);
    }

    @Test
    void removeCoupon_shouldThrowExceptionWhenNoCouponApplied() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        assertThrows(
                InvalidCouponException.class,
                () -> cartService.removeCoupon(
                        "test@example.com"));
    }

    // =========================================================
    // INVALID / EXPIRED COUPON
    // =========================================================

    @Test
    void getCart_shouldRemoveInvalidCouponAutomatically() {

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("EXPIRED")
                .build();

        cart.setCoupon(coupon);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart));

        when(discountService.getBestDiscount(
                eq(1L),
                eq(product.getPrice()),
                eq(2)))
                .thenReturn(noDiscount(product, 2));

        when(couponService.getValidCoupon(
                "EXPIRED",
                new BigDecimal("200.00")))
                .thenThrow(
                        new InvalidCouponException(
                                "Coupon expired"));

        CartResponse response = cartService.getCart("test@example.com");

        assertNull(cart.getCoupon());
        assertNull(response.getCouponCode());
        assertEquals(BigDecimal.ZERO, response.getCouponDiscount());
        assertEquals(
                new BigDecimal("200.00"),
                response.getTotalAmount());

        verify(cartRepository).save(cart);
    }
}