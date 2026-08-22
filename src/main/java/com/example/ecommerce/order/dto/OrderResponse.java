package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.PaymentMethod;
import com.example.ecommerce.order.enums.PaymentStatus;
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

    private BigDecimal subtotal;

    private BigDecimal discountAmount;

    private BigDecimal couponDiscount;

    private BigDecimal totalAmount;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private OrderStatus status;

    private LocalDateTime createdAt;
}