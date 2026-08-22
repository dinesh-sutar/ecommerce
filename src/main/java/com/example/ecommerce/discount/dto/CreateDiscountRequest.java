package com.example.ecommerce.discount.dto;

import com.example.ecommerce.discount.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiscountRequest {

    @NotBlank(message = "Discount name is required")
    private String name;

    @NotBlank(message = "Discount code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType type;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal value;

    @DecimalMin(value = "0.00", message = "Minimum cart value cannot be negative")
    private BigDecimal minCartValue;

    @DecimalMin(value = "0.01", message = "Maximum discount must be greater than zero")
    private BigDecimal maxDiscount;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @Builder.Default
    private Boolean active = true;

    @NotEmpty(message = "At least one product must be selected")
    private List<Long> productIds;
}