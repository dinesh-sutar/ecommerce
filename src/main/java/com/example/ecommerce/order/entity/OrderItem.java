package com.example.ecommerce.order.entity;

import com.example.ecommerce.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /*
     * Reference to the actual product.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /*
     * Product details snapshot at checkout.
     */
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String sku;

    /*
     * Original unit price before discount.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /*
     * Total before product discount.
     *
     * unitPrice × quantity
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /*
     * Product-level discount applied
     * to this order item.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    /*
     * Final item amount after product discount.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;
}