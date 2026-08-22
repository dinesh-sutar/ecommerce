package com.example.ecommerce.order.service;

import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.coupon.entity.Coupon;
import com.example.ecommerce.coupon.service.CouponService;
import com.example.ecommerce.discount.dto.ProductDiscountResult;
import com.example.ecommerce.discount.service.DiscountService;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.exception.InvalidCouponException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.order.dto.CheckoutRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.PaymentMethod;
import com.example.ecommerce.order.enums.PaymentStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DiscountService discountService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private CheckoutRequest checkoutRequest;

    private final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .email(EMAIL)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Test Laptop")
                .sku("LAP-001")
                .price(new BigDecimal("1000.00"))
                .stock(10)
                .category("Electronics")
                .active(true)
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

        checkoutRequest = new CheckoutRequest(PaymentMethod.COD);
    }

    // =========================================================
    // SUCCESSFUL CHECKOUT
    // =========================================================

    @Test
    void checkout_ShouldCreateOrderSuccessfully() {

        ProductDiscountResult discountResult = createDiscountResult(
                new BigDecimal("100.00"),
                new BigDecimal("1900.00"));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(discountService.getBestDiscount(
                eq(product.getId()),
                eq(product.getPrice()),
                eq(cartItem.getQuantity())))
                .thenReturn(discountResult);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);
                    order.setId(100L);

                    return order;
                });

        OrderResponse response = orderService.checkout(EMAIL, checkoutRequest);

        assertNotNull(response);

        assertEquals(100L, response.getOrderId());

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(response.getSubtotal()));

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(response.getDiscountAmount()));

        assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(response.getCouponDiscount()));

        assertEquals(
                0,
                new BigDecimal("1900.00")
                        .compareTo(response.getTotalAmount()));

        assertEquals(
                PaymentMethod.COD,
                response.getPaymentMethod());

        assertEquals(
                PaymentStatus.PENDING,
                response.getPaymentStatus());

        assertEquals(
                OrderStatus.CREATED,
                response.getStatus());

        assertEquals(1, response.getItems().size());

        // Stock: 10 - 2 = 8
        assertEquals(8, product.getStock());

        // Cart should be cleared
        assertTrue(cart.getItems().isEmpty());

        verify(orderRepository).save(any(Order.class));
    }

    // =========================================================
    // USER NOT FOUND
    // =========================================================

    @Test
    void checkout_ShouldThrowException_WhenUserNotFound() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.checkout(
                        EMAIL,
                        checkoutRequest));

        assertEquals(
                "User not found",
                exception.getMessage());

        verify(cartRepository, never())
                .findByUserId(anyLong());

        verify(orderRepository, never())
                .save(any());
    }

    // =========================================================
    // CART NOT FOUND
    // =========================================================

    @Test
    void checkout_ShouldThrowException_WhenCartNotFound() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.checkout(
                        EMAIL,
                        checkoutRequest));

        assertEquals(
                "Cart not found",
                exception.getMessage());

        verify(orderRepository, never())
                .save(any());
    }

    // =========================================================
    // EMPTY CART
    // =========================================================

    @Test
    void checkout_ShouldThrowException_WhenCartIsEmpty() {

        cart.setItems(new ArrayList<>());

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        EMAIL,
                        checkoutRequest));

        assertEquals(
                "Cannot checkout with an empty cart",
                exception.getMessage());

        verify(orderRepository, never())
                .save(any());
    }

    // =========================================================
    // PRODUCT NOT FOUND
    // =========================================================

    @Test
    void checkout_ShouldThrowException_WhenProductNotFound() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.checkout(
                        EMAIL,
                        checkoutRequest));

        verify(orderRepository, never())
                .save(any());
    }

    // =========================================================
    // INACTIVE PRODUCT
    // =========================================================

    @Test
    void checkout_ShouldThrowException_WhenProductIsInactive() {

        product.setActive(false);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        EMAIL,
                        checkoutRequest));

        assertEquals(
                "Product is inactive: Laptop",
                exception.getMessage());

        verify(orderRepository, never())
                .save(any());
    }

    // =========================================================
    // INSUFFICIENT STOCK
    // =========================================================

    @Test
    void checkout_ShouldThrowException_WhenStockIsInsufficient() {

        product.setStock(1);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.checkout(
                        EMAIL,
                        checkoutRequest));

        verify(orderRepository, never())
                .save(any());
    }

    // =========================================================
    // CHECKOUT WITH COUPON
    // =========================================================

    @Test
    void checkout_ShouldApplyCouponDiscount() {

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("SAVE100")
                .active(true)
                .expiryDate(
                        LocalDateTime.now().plusDays(5))
                .build();

        cart.setCoupon(coupon);

        ProductDiscountResult discountResult = createDiscountResult(
                BigDecimal.ZERO,
                new BigDecimal("2000.00"));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(discountService.getBestDiscount(
                anyLong(),
                any(BigDecimal.class),
                anyInt()))
                .thenReturn(discountResult);

        when(couponService.getValidCoupon(
                eq("SAVE100"),
                eq(new BigDecimal("2000.00"))))
                .thenReturn(coupon);

        when(couponService.calculateCouponDiscount(
                eq(coupon),
                eq(new BigDecimal("2000.00"))))
                .thenReturn(new BigDecimal("100.00"));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);
                    order.setId(100L);

                    return order;
                });

        OrderResponse response = orderService.checkout(EMAIL, checkoutRequest);

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(response.getCouponDiscount()));

        assertEquals(
                0,
                new BigDecimal("1900.00")
                        .compareTo(response.getTotalAmount()));

        // Coupon should be removed after checkout
        assertNull(cart.getCoupon());

        assertTrue(cart.getItems().isEmpty());
    }

    // =========================================================
    // INVALID COUPON DURING CHECKOUT
    // =========================================================

    @Test
    void checkout_ShouldThrowException_WhenCouponBecomesInvalid() {

        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("EXPIRED")
                .active(true)
                .build();

        cart.setCoupon(coupon);

        ProductDiscountResult discountResult = createDiscountResult(
                BigDecimal.ZERO,
                new BigDecimal("2000.00"));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(discountService.getBestDiscount(
                anyLong(),
                any(BigDecimal.class),
                anyInt()))
                .thenReturn(discountResult);

        when(couponService.getValidCoupon(
                anyString(),
                any(BigDecimal.class)))
                .thenThrow(
                        new InvalidCouponException(
                                "Coupon has expired"));

        assertThrows(
                InvalidCouponException.class,
                () -> orderService.checkout(
                        EMAIL,
                        checkoutRequest));

        // Cart should not be cleared because checkout failed
        assertFalse(cart.getItems().isEmpty());

        verify(orderRepository, never())
                .save(any());
    }

    // =========================================================
    // PRODUCT DISCOUNT CALCULATION
    // =========================================================

    @Test
    void checkout_ShouldCalculateCorrectProductDiscount() {

        ProductDiscountResult discountResult = createDiscountResult(
                new BigDecimal("400.00"),
                new BigDecimal("1600.00"));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(discountService.getBestDiscount(
                anyLong(),
                any(BigDecimal.class),
                anyInt()))
                .thenReturn(discountResult);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);
                    order.setId(100L);

                    return order;
                });

        OrderResponse response = orderService.checkout(EMAIL, checkoutRequest);

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(response.getSubtotal()));

        assertEquals(
                0,
                new BigDecimal("400.00")
                        .compareTo(response.getDiscountAmount()));

        assertEquals(
                0,
                new BigDecimal("1600.00")
                        .compareTo(response.getTotalAmount()));
    }

    // =========================================================
    // INVENTORY REDUCTION
    // =========================================================

    @Test
    void checkout_ShouldReduceInventory() {

        ProductDiscountResult discountResult = createDiscountResult(
                BigDecimal.ZERO,
                new BigDecimal("2000.00"));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        /*
         * OrderService calls findById twice:
         *
         * 1. Product validation and pricing
         * 2. Inventory reduction
         */
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(discountService.getBestDiscount(
                anyLong(),
                any(BigDecimal.class),
                anyInt()))
                .thenReturn(discountResult);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);
                    order.setId(100L);

                    return order;
                });

        orderService.checkout(EMAIL, checkoutRequest);

        assertEquals(
                8,
                product.getStock());

        verify(productRepository, times(2))
                .findById(product.getId());
    }

    // =========================================================
    // CART CLEARED AFTER SUCCESSFUL CHECKOUT
    // =========================================================

    @Test
    void checkout_ShouldClearCartAfterSuccessfulCheckout() {

        ProductDiscountResult discountResult = createDiscountResult(
                BigDecimal.ZERO,
                new BigDecimal("2000.00"));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(discountService.getBestDiscount(
                anyLong(),
                any(BigDecimal.class),
                anyInt()))
                .thenReturn(discountResult);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);
                    order.setId(100L);

                    return order;
                });

        orderService.checkout(EMAIL, checkoutRequest);

        assertTrue(cart.getItems().isEmpty());

        assertNull(cart.getCoupon());
    }

    // =========================================================
    // VERIFY ORDER DETAILS
    // =========================================================

    @Test
    void checkout_ShouldCreateCorrectOrderAndOrderItems() {

        ProductDiscountResult discountResult = createDiscountResult(
                new BigDecimal("200.00"),
                new BigDecimal("1800.00"));

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(cart));

        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        when(discountService.getBestDiscount(
                anyLong(),
                any(BigDecimal.class),
                anyInt()))
                .thenReturn(discountResult);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order = invocation.getArgument(0);
                    order.setId(100L);

                    return order;
                });

        orderService.checkout(EMAIL, checkoutRequest);

        verify(orderRepository)
                .save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        assertEquals(user, savedOrder.getUser());

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(savedOrder.getSubtotal()));

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(savedOrder.getDiscountAmount()));

        assertEquals(
                0,
                new BigDecimal("1800.00")
                        .compareTo(savedOrder.getTotalAmount()));

        assertEquals(
                PaymentMethod.COD,
                savedOrder.getPaymentMethod());

        assertEquals(
                PaymentStatus.PENDING,
                savedOrder.getPaymentStatus());

        assertEquals(
                OrderStatus.CREATED,
                savedOrder.getStatus());

        assertEquals(
                1,
                savedOrder.getItems().size());

        assertEquals(
                savedOrder,
                savedOrder.getItems()
                        .get(0)
                        .getOrder());
    }

    // =========================================================
    // GET USER ORDERS
    // =========================================================

    @Test
    void getUserOrders_ShouldReturnUserOrders() {

        Order order = createOrder(100L);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId()))
                .thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getUserOrders(EMAIL);

        assertEquals(1, responses.size());

        assertEquals(
                100L,
                responses.get(0).getOrderId());

        verify(orderRepository)
                .findByUserIdOrderByCreatedAtDesc(
                        user.getId());
    }

    // =========================================================
    // GET USER ORDERS - USER NOT FOUND
    // =========================================================

    @Test
    void getUserOrders_ShouldThrowException_WhenUserNotFound() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getUserOrders(EMAIL));

        verify(orderRepository, never())
                .findByUserIdOrderByCreatedAtDesc(anyLong());
    }

    // =========================================================
    // GET ORDER BY ID
    // =========================================================

    @Test
    void getOrderById_ShouldReturnOrder() {

        Order order = createOrder(100L);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(orderRepository.findByIdAndUserId(
                100L,
                user.getId()))
                .thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(
                EMAIL,
                100L);

        assertNotNull(response);

        assertEquals(
                100L,
                response.getOrderId());
    }

    // =========================================================
    // GET ORDER BY ID - ORDER NOT FOUND
    // =========================================================

    @Test
    void getOrderById_ShouldThrowException_WhenOrderNotFound() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(orderRepository.findByIdAndUserId(
                999L,
                user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrderById(
                        EMAIL,
                        999L));
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private ProductDiscountResult createDiscountResult(
            BigDecimal discountAmount,
            BigDecimal finalAmount) {

        /*
         * If your ProductDiscountResult has additional fields,
         * adjust this builder according to your DTO.
         */

        return ProductDiscountResult.builder()
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    private Order createOrder(Long orderId) {

        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .subtotal(new BigDecimal("2000.00"))
                .discountAmount(new BigDecimal("100.00"))
                .couponDiscount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1900.00"))
                .status(OrderStatus.CREATED)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        return order;
    }
}