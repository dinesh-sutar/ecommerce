package com.example.ecommerce.discount.controller;

import com.example.ecommerce.discount.dto.CreateDiscountRequest;
import com.example.ecommerce.discount.dto.DiscountResponse;
import com.example.ecommerce.discount.enums.DiscountType;
import com.example.ecommerce.discount.service.DiscountService;
import com.example.ecommerce.exception.DiscountAlreadyExistsException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.security.JwtService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DiscountController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "test@example.com")
class DiscountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiscountService discountService;

    @MockBean
    private JwtService jwtService;

    private CreateDiscountRequest validRequest;

    private DiscountResponse discountResponse;

    @BeforeEach
    void setUp() {
        validRequest = CreateDiscountRequest.builder()
                .name("Festive Sale")
                .code("FEST10")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN)
                .minCartValue(BigDecimal.valueOf(500))
                .maxDiscount(BigDecimal.valueOf(200))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(7))
                .active(true)
                .productIds(List.of(1L, 2L))
                .build();

        discountResponse = DiscountResponse.builder()
                .id(1L)
                .name("Festive Sale")
                .code("FEST10")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN)
                .minCartValue(BigDecimal.valueOf(500))
                .maxDiscount(BigDecimal.valueOf(200))
                .startDate(validRequest.getStartDate())
                .endDate(validRequest.getEndDate())
                .active(true)
                .productIds(List.of(1L, 2L))
                .build();
    }

    @Test
    void createDiscount_withValidRequest_returns201() throws Exception {
        when(discountService.createDiscount(any()))
                .thenReturn(discountResponse);

        mockMvc.perform(post("/api/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("FEST10"))
                .andExpect(jsonPath("$.productIds[0]").value(1));
    }

    @Test
    void createDiscount_withoutProducts_returns400() throws Exception {
        validRequest.setProductIds(List.of());

        mockMvc.perform(post("/api/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDiscount_withMissingValue_returns400() throws Exception {
        validRequest.setValue(null);

        mockMvc.perform(post("/api/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDiscount_withDuplicateCode_returns409() throws Exception {
        when(discountService.createDiscount(any()))
                .thenThrow(new DiscountAlreadyExistsException(
                        "Discount code already exists"));

        mockMvc.perform(post("/api/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Discount code already exists"));
    }

    @Test
    void createDiscount_referencingMissingProduct_returns404() throws Exception {
        when(discountService.createDiscount(any()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(post("/api/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }
}