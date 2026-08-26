package com.example.ecommerce.order.entity;

import com.example.ecommerce.address.entity.UserAddress;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.PaymentMethod;
import com.example.ecommerce.order.enums.PaymentStatus;
import com.example.ecommerce.order.enums.ShippingType;
import com.example.ecommerce.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Total before product discounts
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Total product-level discount
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    // Coupon discount
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal couponDiscount;

    // Final amount payable
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShippingType shippingType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private UserAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}