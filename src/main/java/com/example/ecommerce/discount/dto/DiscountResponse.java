package com.example.ecommerce.discount.dto;

import com.example.ecommerce.discount.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountResponse {

    private Long id;

    private String name;

    private String code;

    private DiscountType type;

    private BigDecimal value;

    private BigDecimal minCartValue;

    private BigDecimal maxDiscount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean active;

    private List<Long> productIds;
}