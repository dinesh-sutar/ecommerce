package com.example.ecommerce.order.controller;

import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.order.dto.CheckoutRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.enums.OrderStatus;
import com.example.ecommerce.order.enums.PaymentMethod;
import com.example.ecommerce.order.enums.PaymentStatus;
import com.example.ecommerce.order.service.OrderService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@WithMockUser(username = "test@example.com", roles = "USER")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {

        orderResponse = OrderResponse.builder()
                .orderId(1L)
                .items(List.of())
                .subtotal(BigDecimal.valueOf(1598))
                .discountAmount(BigDecimal.ZERO)
                .couponDiscount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(1598))
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void checkout_withValidRequest_returns201() throws Exception {

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.COD);

        when(orderService.checkout(
                anyString(),
                any(CheckoutRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void checkout_withMissingPaymentMethod_returns400()
            throws Exception {

        CheckoutRequest request = new CheckoutRequest();

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_withEmptyCart_returns400()
            throws Exception {

        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.COD);

        when(orderService.checkout(
                anyString(),
                any(CheckoutRequest.class))).thenThrow(
                        new IllegalArgumentException("Cart is empty"));

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Cart is empty"));
    }

    @Test
    void getUserOrders_returnsList() throws Exception {

        when(orderService.getUserOrders(anyString()))
                .thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].orderId").value(1));
    }

    @Test
    void getOrderById_whenExists_returnsOrder()
            throws Exception {

        when(orderService.getOrderById(
                anyString(),
                anyLong())).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orderId").value(1))
                .andExpect(
                        jsonPath("$.totalAmount").value(1598));
    }

    @Test
    void getOrderById_whenMissing_returns404()
            throws Exception {

        when(orderService.getOrderById(
                anyString(),
                anyLong())).thenThrow(
                        new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_belongingToAnotherUser_returns404()
            throws Exception {

        when(orderService.getOrderById(
                anyString(),
                anyLong())).thenThrow(
                        new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/2"))
                .andExpect(status().isNotFound());
    }
}