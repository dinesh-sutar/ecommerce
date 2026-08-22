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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DiscountItemRepository discountItemRepository;

    @InjectMocks
    private DiscountService discountService;

    private Product product;
    private Discount validDiscount;

    @BeforeEach
    void setUp() {

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Gaming Laptop")
                .sku("LAPTOP-001")
                .price(new BigDecimal("1000"))
                .stock(10)
                .category("Electronics")
                .active(true)
                .build();

        validDiscount = Discount.builder()
                .id(1L)
                .name("Summer Sale")
                .code("SUMMER10")
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .active(true)
                .items(new ArrayList<>())
                .build();
    }

    // =========================================================
    // CREATE DISCOUNT TESTS
    // =========================================================

    @Test
    void createDiscount_ShouldCreateSuccessfully() {

        CreateDiscountRequest request = createValidRequest(List.of(1L));

        when(discountRepository.existsByCode("SUMMER10"))
                .thenReturn(false);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> {

                    Discount discount = invocation.getArgument(0);
                    discount.setId(1L);

                    return discount;
                });

        DiscountResponse response = discountService.createDiscount(request);

        assertNotNull(response);

        assertEquals(1L, response.getId());
        assertEquals("Summer Sale", response.getName());
        assertEquals("SUMMER10", response.getCode());
        assertEquals(DiscountType.PERCENTAGE, response.getType());
        assertEquals(
                0,
                new BigDecimal("10")
                        .compareTo(response.getValue()));

        assertEquals(1, response.getProductIds().size());
        assertEquals(1L, response.getProductIds().get(0));

        verify(discountRepository)
                .existsByCode("SUMMER10");

        verify(productRepository)
                .findById(1L);

        verify(discountRepository)
                .save(any(Discount.class));
    }

    @Test
    void createDiscount_ShouldThrowException_WhenCodeAlreadyExists() {

        CreateDiscountRequest request = createValidRequest(List.of(1L));

        when(discountRepository.existsByCode("SUMMER10"))
                .thenReturn(true);

        assertThrows(
                DiscountAlreadyExistsException.class,
                () -> discountService.createDiscount(request));

        verify(discountRepository, never())
                .save(any());

        verify(productRepository, never())
                .findById(anyLong());
    }

    @Test
    void createDiscount_ShouldThrowException_WhenEndDateEqualsStartDate() {

        LocalDateTime date = LocalDateTime.now().plusDays(1);

        CreateDiscountRequest request = createValidRequest(List.of(1L));

        request.setStartDate(date);
        request.setEndDate(date);

        when(discountRepository.existsByCode("SUMMER10"))
                .thenReturn(false);

        assertThrows(
                InvalidDiscountException.class,
                () -> discountService.createDiscount(request));

        verify(discountRepository, never())
                .save(any());
    }

    @Test
    void createDiscount_ShouldThrowException_WhenEndDateBeforeStartDate() {

        CreateDiscountRequest request = createValidRequest(List.of(1L));

        request.setStartDate(
                LocalDateTime.now().plusDays(10));

        request.setEndDate(
                LocalDateTime.now().plusDays(5));

        when(discountRepository.existsByCode("SUMMER10"))
                .thenReturn(false);

        assertThrows(
                InvalidDiscountException.class,
                () -> discountService.createDiscount(request));

        verify(discountRepository, never())
                .save(any());
    }

    @Test
    void createDiscount_ShouldThrowException_WhenPercentageGreaterThan100() {

        CreateDiscountRequest request = createValidRequest(List.of(1L));

        request.setType(DiscountType.PERCENTAGE);
        request.setValue(new BigDecimal("101"));

        when(discountRepository.existsByCode("SUMMER10"))
                .thenReturn(false);

        assertThrows(
                InvalidDiscountException.class,
                () -> discountService.createDiscount(request));

        verify(discountRepository, never())
                .save(any());
    }

    @Test
    void createDiscount_ShouldThrowException_WhenProductNotFound() {

        CreateDiscountRequest request = createValidRequest(List.of(99L));

        when(discountRepository.existsByCode("SUMMER10"))
                .thenReturn(false);

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> discountService.createDiscount(request));

        verify(discountRepository, never())
                .save(any());
    }

    @Test
    void createDiscount_ShouldAddMultipleProducts() {

        Product secondProduct = Product.builder()
                .id(2L)
                .name("Phone")
                .description("Smart Phone")
                .sku("PHONE-001")
                .price(new BigDecimal("500"))
                .stock(10)
                .category("Electronics")
                .active(true)
                .build();

        CreateDiscountRequest request = createValidRequest(List.of(1L, 2L));

        when(discountRepository.existsByCode("SUMMER10"))
                .thenReturn(false);

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(secondProduct));

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> {

                    Discount discount = invocation.getArgument(0);
                    discount.setId(1L);

                    return discount;
                });

        DiscountResponse response = discountService.createDiscount(request);

        assertEquals(2, response.getProductIds().size());

        assertTrue(
                response.getProductIds().contains(1L));

        assertTrue(
                response.getProductIds().contains(2L));

        verify(productRepository)
                .findById(1L);

        verify(productRepository)
                .findById(2L);
    }

    // =========================================================
    // IS DISCOUNT VALID TESTS
    // =========================================================

    @Test
    void isDiscountValid_ShouldReturnTrue_WhenDiscountIsActiveAndWithinDateRange() {

        boolean result = discountService.isDiscountValid(validDiscount);

        assertTrue(result);
    }

    @Test
    void isDiscountValid_ShouldReturnFalse_WhenDiscountIsInactive() {

        validDiscount.setActive(false);

        boolean result = discountService.isDiscountValid(validDiscount);

        assertFalse(result);
    }

    @Test
    void isDiscountValid_ShouldReturnFalse_WhenDiscountHasNotStarted() {

        validDiscount.setStartDate(
                LocalDateTime.now().plusDays(1));

        validDiscount.setEndDate(
                LocalDateTime.now().plusDays(5));

        boolean result = discountService.isDiscountValid(validDiscount);

        assertFalse(result);
    }

    @Test
    void isDiscountValid_ShouldReturnFalse_WhenDiscountHasExpired() {

        validDiscount.setStartDate(
                LocalDateTime.now().minusDays(10));

        validDiscount.setEndDate(
                LocalDateTime.now().minusDays(1));

        boolean result = discountService.isDiscountValid(validDiscount);

        assertFalse(result);
    }

    // =========================================================
    // CALCULATE DISCOUNT AMOUNT TESTS
    // =========================================================

    @Test
    void calculateDiscountAmount_ShouldCalculatePercentageDiscount() {

        validDiscount.setType(DiscountType.PERCENTAGE);
        validDiscount.setValue(new BigDecimal("10"));

        BigDecimal result = discountService.calculateDiscountAmount(
                validDiscount,
                new BigDecimal("100"),
                2);

        assertEquals(
                0,
                new BigDecimal("20").compareTo(result));
    }

    @Test
    void calculateDiscountAmount_ShouldCalculateFixedDiscount() {

        validDiscount.setType(DiscountType.FLAT);
        validDiscount.setValue(new BigDecimal("50"));

        BigDecimal result = discountService.calculateDiscountAmount(
                validDiscount,
                new BigDecimal("100"),
                2);

        assertEquals(
                0,
                new BigDecimal("50").compareTo(result));
    }

    @Test
    void calculateDiscountAmount_ShouldReturnZero_WhenMinimumValueNotReached() {

        validDiscount.setMinCartValue(
                new BigDecimal("500"));

        BigDecimal result = discountService.calculateDiscountAmount(
                validDiscount,
                new BigDecimal("100"),
                2);

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateDiscountAmount_ShouldApplyMaximumDiscountCap() {

        validDiscount.setType(DiscountType.PERCENTAGE);
        validDiscount.setValue(new BigDecimal("50"));
        validDiscount.setMaxDiscount(new BigDecimal("100"));

        BigDecimal result = discountService.calculateDiscountAmount(
                validDiscount,
                new BigDecimal("1000"),
                1);

        assertEquals(
                0,
                new BigDecimal("100").compareTo(result));
    }

    @Test
    void calculateDiscountAmount_ShouldNotExceedTotalPrice() {

        validDiscount.setType(DiscountType.FLAT);
        validDiscount.setValue(new BigDecimal("500"));

        BigDecimal result = discountService.calculateDiscountAmount(
                validDiscount,
                new BigDecimal("100"),
                1);

        assertEquals(
                0,
                new BigDecimal("100").compareTo(result));
    }

    // =========================================================
    // GET BEST DISCOUNT TESTS
    // =========================================================

    @Test
    void getBestDiscount_ShouldReturnNoDiscount_WhenNoDiscountExists() {

        when(discountItemRepository.findByProductId(1L))
                .thenReturn(List.of());

        ProductDiscountResult result = discountService.getBestDiscount(
                1L,
                new BigDecimal("100"),
                2);

        assertFalse(result.isApplied());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.getDiscountAmount()));

        assertEquals(
                0,
                new BigDecimal("200")
                        .compareTo(result.getFinalAmount()));
    }

    @Test
    void getBestDiscount_ShouldIgnoreInvalidDiscount() {

        Discount expiredDiscount = Discount.builder()
                .id(1L)
                .code("EXPIRED")
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("50"))
                .startDate(
                        LocalDateTime.now()
                                .minusDays(10))
                .endDate(
                        LocalDateTime.now()
                                .minusDays(1))
                .active(true)
                .build();

        DiscountItem discountItem = DiscountItem.builder()
                .discount(expiredDiscount)
                .product(product)
                .build();

        when(discountItemRepository.findByProductId(1L))
                .thenReturn(List.of(discountItem));

        ProductDiscountResult result = discountService.getBestDiscount(
                1L,
                new BigDecimal("100"),
                1);

        assertFalse(result.isApplied());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.getDiscountAmount()));

        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(result.getFinalAmount()));
    }

    @Test
    void getBestDiscount_ShouldChooseHighestDiscount() {

        Discount discount10 = createDiscount(
                1L,
                "DISCOUNT10",
                DiscountType.PERCENTAGE,
                "10");

        Discount discount20 = createDiscount(
                2L,
                "DISCOUNT20",
                DiscountType.PERCENTAGE,
                "20");

        DiscountItem item1 = DiscountItem.builder()
                .discount(discount10)
                .product(product)
                .build();

        DiscountItem item2 = DiscountItem.builder()
                .discount(discount20)
                .product(product)
                .build();

        when(discountItemRepository.findByProductId(1L))
                .thenReturn(List.of(item1, item2));

        ProductDiscountResult result = discountService.getBestDiscount(
                1L,
                new BigDecimal("100"),
                1);

        assertTrue(result.isApplied());

        assertEquals(
                2L,
                result.getDiscountId());

        assertEquals(
                "DISCOUNT20",
                result.getDiscountCode());

        assertEquals(
                0,
                new BigDecimal("20")
                        .compareTo(
                                result.getDiscountAmount()));

        assertEquals(
                0,
                new BigDecimal("80")
                        .compareTo(
                                result.getFinalAmount()));
    }

    @Test
    void getBestDiscount_ShouldIgnoreDiscount_WhenMinimumValueNotReached() {

        Discount discount = createDiscount(
                1L,
                "MIN500",
                DiscountType.PERCENTAGE,
                "50");

        discount.setMinCartValue(
                new BigDecimal("500"));

        DiscountItem discountItem = DiscountItem.builder()
                .discount(discount)
                .product(product)
                .build();

        when(discountItemRepository.findByProductId(1L))
                .thenReturn(List.of(discountItem));

        ProductDiscountResult result = discountService.getBestDiscount(
                1L,
                new BigDecimal("100"),
                1);

        assertFalse(result.isApplied());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.getDiscountAmount()));

        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(
                                result.getFinalAmount()));
    }

    @Test
    void getBestDiscount_ShouldApplyMaximumDiscountCap() {

        Discount discount = createDiscount(
                1L,
                "MAX100",
                DiscountType.PERCENTAGE,
                "50");

        discount.setMaxDiscount(
                new BigDecimal("100"));

        DiscountItem discountItem = DiscountItem.builder()
                .discount(discount)
                .product(product)
                .build();

        when(discountItemRepository.findByProductId(1L))
                .thenReturn(List.of(discountItem));

        ProductDiscountResult result = discountService.getBestDiscount(
                1L,
                new BigDecimal("1000"),
                1);

        assertTrue(result.isApplied());

        assertEquals(
                0,
                new BigDecimal("100")
                        .compareTo(
                                result.getDiscountAmount()));

        assertEquals(
                0,
                new BigDecimal("900")
                        .compareTo(
                                result.getFinalAmount()));
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private CreateDiscountRequest createValidRequest(
            List<Long> productIds) {

        return CreateDiscountRequest.builder()
                .name("Summer Sale")
                .code("SUMMER10")
                .type(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10"))
                .minCartValue(new BigDecimal("100"))
                .maxDiscount(new BigDecimal("500"))
                .startDate(
                        LocalDateTime.now().plusDays(1))
                .endDate(
                        LocalDateTime.now().plusDays(10))
                .active(true)
                .productIds(productIds)
                .build();
    }

    private Discount createDiscount(
            Long id,
            String code,
            DiscountType type,
            String value) {

        return Discount.builder()
                .id(id)
                .name(code)
                .code(code)
                .type(type)
                .value(new BigDecimal(value))
                .startDate(
                        LocalDateTime.now()
                                .minusDays(1))
                .endDate(
                        LocalDateTime.now()
                                .plusDays(10))
                .active(true)
                .items(new ArrayList<>())
                .build();
    }
}