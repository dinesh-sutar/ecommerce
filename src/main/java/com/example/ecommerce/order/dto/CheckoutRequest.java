package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}