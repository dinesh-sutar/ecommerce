package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.PaymentMethod;
import com.example.ecommerce.order.enums.PaymentStatus;
import com.example.ecommerce.order.enums.ShippingType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;

    private List<OrderItemResponse> items;

    // Product total before discounts
    private BigDecimal subtotal;

    // Total product-level discount
    private BigDecimal discountAmount;

    // Coupon discount
    private BigDecimal couponDiscount;

    // Shipping information
    private ShippingType shippingType;

    private BigDecimal shippingCost;

    // Final payable amount
    private BigDecimal totalAmount;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private OrderStatus status;

    private LocalDateTime createdAt;
}