package com.example.ecommerce.discount.service;

import com.example.ecommerce.discount.dto.CreateDiscountRequest;
import com.example.ecommerce.discount.dto.DiscountResponse;
import com.example.ecommerce.discount.dto.ProductDiscountResult;
import com.example.ecommerce.discount.entity.Discount;
import com.example.ecommerce.discount.entity.DiscountItem;
import com.example.ecommerce.discount.enums.DiscountType;
import com.example.ecommerce.discount.repository.DiscountItemRepository;
import com.example.ecommerce.discount.repository.DiscountRepository;
import com.example.ecommerce.exception.DiscountAlreadyExistsException;
import com.example.ecommerce.exception.InvalidDiscountException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;
    private final DiscountItemRepository discountItemRepository;

    @Transactional
    public DiscountResponse createDiscount(CreateDiscountRequest request) {

        // Check duplicate discount code
        if (discountRepository.existsByCode(request.getCode())) {
            throw new DiscountAlreadyExistsException(
                    "Discount code already exists: " + request.getCode());
        }

        // Validate dates
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new InvalidDiscountException(
                    "End date must be after start date");
        }

        // Validate discount value
        if (request.getType() == DiscountType.PERCENTAGE
                && request.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new InvalidDiscountException(
                    "Percentage discount cannot be greater than 100");
        }

        // Create Discount
        Discount discount = Discount.builder()
                .name(request.getName())
                .code(request.getCode())
                .type(request.getType())
                .value(request.getValue())
                .minCartValue(request.getMinCartValue())
                .maxDiscount(request.getMaxDiscount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.getActive())
                .items(new ArrayList<>())
                .build();

        // Add products
        for (Long productId : request.getProductIds()) {

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + productId));

            DiscountItem discountItem = DiscountItem.builder()
                    .discount(discount)
                    .product(product)
                    .build();

            discount.getItems().add(discountItem);
        }

        Discount savedDiscount = discountRepository.save(discount);

        return mapToResponse(savedDiscount);
    }

    public boolean isDiscountValid(Discount discount) {

        LocalDateTime now = LocalDateTime.now();

        return Boolean.TRUE.equals(discount.getActive())
                && !now.isBefore(discount.getStartDate())
                && !now.isAfter(discount.getEndDate());
    }

    public BigDecimal calculateDiscountAmount(
            Discount discount,
            BigDecimal productPrice,
            Integer quantity) {

        BigDecimal totalPrice = productPrice.multiply(BigDecimal.valueOf(quantity));

        // Minimum cart/item value validation
        if (discount.getMinCartValue() != null
                && totalPrice.compareTo(discount.getMinCartValue()) < 0) {

            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount;

        if (discount.getType() == DiscountType.PERCENTAGE) {

            discountAmount = totalPrice
                    .multiply(discount.getValue())
                    .divide(BigDecimal.valueOf(100));

        } else {

            discountAmount = discount.getValue();
        }

        // Apply maximum discount cap
        if (discount.getMaxDiscount() != null
                && discountAmount.compareTo(discount.getMaxDiscount()) > 0) {

            discountAmount = discount.getMaxDiscount();
        }

        // Discount should never exceed actual total
        if (discountAmount.compareTo(totalPrice) > 0) {
            discountAmount = totalPrice;
        }

        return discountAmount;
    }

    private DiscountResponse mapToResponse(Discount discount) {

        List<Long> productIds = discount.getItems()
                .stream()
                .map(item -> item.getProduct().getId())
                .toList();

        return DiscountResponse.builder()
                .id(discount.getId())
                .name(discount.getName())
                .code(discount.getCode())
                .type(discount.getType())
                .value(discount.getValue())
                .minCartValue(discount.getMinCartValue())
                .maxDiscount(discount.getMaxDiscount())
                .startDate(discount.getStartDate())
                .endDate(discount.getEndDate())
                .active(discount.getActive())
                .productIds(productIds)
                .build();
    }

    public ProductDiscountResult getBestDiscount(
        Long productId,
        BigDecimal productPrice,
        Integer quantity
) {

    BigDecimal totalPrice =
            productPrice.multiply(BigDecimal.valueOf(quantity));

    List<DiscountItem> discountItems =
            discountItemRepository.findByProductId(productId);

    Discount bestDiscount = null;
    BigDecimal highestDiscountAmount = BigDecimal.ZERO;

    for (DiscountItem discountItem : discountItems) {

        Discount discount = discountItem.getDiscount();

        // Skip inactive, expired, or not-yet-started discounts
        if (!isDiscountValid(discount)) {
            continue;
        }

        BigDecimal discountAmount =
                calculateDiscountAmount(
                        discount,
                        productPrice,
                        quantity
                );

        // Skip discounts that are not applicable
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            continue;
        }

        // Select the discount with the highest benefit
        if (discountAmount.compareTo(highestDiscountAmount) > 0) {

            highestDiscountAmount = discountAmount;
            bestDiscount = discount;
        }
    }

    if (bestDiscount == null) {

        return ProductDiscountResult.builder()
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(totalPrice)
                .applied(false)
                .build();
    }

    return ProductDiscountResult.builder()
            .discountId(bestDiscount.getId())
            .discountCode(bestDiscount.getCode())
            .discountAmount(highestDiscountAmount)
            .finalAmount(totalPrice.subtract(highestDiscountAmount))
            .applied(true)
            .build();
}
}