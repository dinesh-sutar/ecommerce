package com.example.ecommerce.coupon.dto;

import com.example.ecommerce.discount.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Coupon type is required")
    private DiscountType type;

    @NotNull(message = "Coupon value is required")
    @DecimalMin(value = "0.01", message = "Coupon value must be greater than zero")
    private BigDecimal value;

    @DecimalMin(value = "0.00", message = "Minimum cart value cannot be negative")
    private BigDecimal minimumCartValue;

    @DecimalMin(value = "0.01", message = "Maximum discount must be greater than zero")
    private BigDecimal maximumDiscount;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiryDate;

    @Builder.Default
    private Boolean active = true;
}