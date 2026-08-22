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
import com.example.ecommerce.order.dto.OrderItemResponse;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderItem;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.PaymentStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DiscountService discountService;
    private final CouponService couponService;

    @Transactional
    public OrderResponse checkout(
            String email,
            CheckoutRequest request) {

        // 1. Get logged-in user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Get user's cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        // 3. Cart must not be empty
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot checkout with an empty cart");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        // 4. Revalidate every product
        for (CartItem cartItem : cart.getItems()) {

            /*
             * Fetch the product again from database.
             * Never trust the old cart product state.
             */
            Product product = productRepository
                    .findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: "
                                    + cartItem.getProduct().getId()));

            // Product must be active
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "Product is inactive: " + product.getName());
            }

            // Revalidate stock
            if (cartItem.getQuantity() > product.getStock()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: "
                                + product.getName());
            }

            // Calculate latest product discount
            ProductDiscountResult discountResult = discountService.getBestDiscount(
                    product.getId(),
                    product.getPrice(),
                    cartItem.getQuantity());

            BigDecimal itemSubtotal = product.getPrice()
                    .multiply(
                            BigDecimal.valueOf(
                                    cartItem.getQuantity()));

            subtotal = subtotal.add(itemSubtotal);

            totalDiscountAmount = totalDiscountAmount
                    .add(discountResult.getDiscountAmount());

            // Create order item snapshot
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .sku(product.getSku())
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(itemSubtotal)
                    .discountAmount(
                            discountResult.getDiscountAmount())
                    .totalPrice(
                            discountResult.getFinalAmount())
                    .build();

            orderItems.add(orderItem);
        }

        // Amount after product-level discounts
        BigDecimal amountAfterDiscount = subtotal.subtract(totalDiscountAmount);

        BigDecimal couponDiscount = BigDecimal.ZERO;

        /*
         * 5. Revalidate coupon at checkout.
         * A coupon may have expired or been disabled
         * after it was added to the cart.
         */
        if (cart.getCoupon() != null) {

            Coupon coupon = cart.getCoupon();

            try {

                Coupon validCoupon = couponService.getValidCoupon(
                        coupon.getCode(),
                        amountAfterDiscount);

                couponDiscount = couponService.calculateCouponDiscount(
                        validCoupon,
                        amountAfterDiscount);

            } catch (InvalidCouponException ex) {

                /*
                 * Important:
                 * At checkout, do NOT silently remove the coupon
                 * and continue charging a different amount.
                 *
                 * Stop checkout and let the user review the cart.
                 */
                throw ex;
            }
        }

        BigDecimal totalAmount = amountAfterDiscount
                .subtract(couponDiscount);

        // 6. Create order
        Order order = Order.builder()
                .user(user)
                .subtotal(subtotal)
                .discountAmount(totalDiscountAmount)
                .couponDiscount(couponDiscount)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        // Set both sides of relationship
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        /*
         * 7. Reduce inventory.
         * Because the entire method is transactional,
         * failures later in the method will roll this back.
         */
        for (CartItem cartItem : cart.getItems()) {

            Product product = productRepository
                    .findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found"));

            product.setStock(
                    product.getStock()
                            - cartItem.getQuantity());
        }

        /*
         * 8. Clear cart and remove coupon.
         */
        cart.getItems().clear();
        cart.setCoupon(null);

        /*
         * 9. Save order.
         * OrderItems are saved automatically because
         * of CascadeType.ALL.
         */
        Order savedOrder = orderRepository.save(order);

        return mapToOrderResponse(savedOrder);
    }

    private OrderResponse mapToOrderResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(
                                item.getProduct().getId())
                        .productName(
                                item.getProductName())
                        .sku(item.getSku())
                        .unitPrice(
                                item.getUnitPrice())
                        .quantity(
                                item.getQuantity())
                        .subtotal(
                                item.getSubtotal())
                        .discountAmount(
                                item.getDiscountAmount())
                        .totalPrice(
                                item.getTotalPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .items(itemResponses)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .couponDiscount(order.getCouponDiscount())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId());

        return orders.stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(
            String email,
            Long orderId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository
                .findByIdAndUserId(
                        orderId,
                        user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found"));

        return mapToOrderResponse(order);
    }
}