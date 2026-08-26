package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.enums.PaymentMethod;
import com.example.ecommerce.order.enums.ShippingType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Address is required")
    private Long addressId;

    @NotNull(message = "Shipping type is required")
    private ShippingType shippingType;
}