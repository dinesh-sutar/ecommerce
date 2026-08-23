package com.example.ecommerce.product.controller;

import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.product.dto.CreateProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.security.JwtService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "test@example.com")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(BigDecimal.valueOf(799))
                .stock(50)
                .category("Electronics")
                .build();
    }

    @Test
    void getProducts_returnsPagedResults() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(
                List.of(productResponse),
                PageRequest.of(0, 10),
                1);

        when(productService.getProducts(isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getProducts_withSearchAndCategory_returnsFilteredResults() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(
                List.of(productResponse),
                PageRequest.of(0, 10),
                1);

        when(productService.getProducts(eq("mouse"), eq("Electronics"), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/products")
                .param("search", "mouse")
                .param("category", "Electronics")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("Electronics"));
    }

    @Test
    void getProductById_whenExists_returnsProduct() throws Exception {
        when(productService.getProductById(anyLong()))
                .thenReturn(productResponse);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.price").value(799));
    }

    @Test
    void getProductById_whenMissing_returns404() throws Exception {
        when(productService.getProductById(anyLong()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void createProduct_withValidRequest_returns201() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Wireless Mouse");
        request.setDescription("Ergonomic wireless mouse");
        request.setSku("WM-001");
        request.setPrice(BigDecimal.valueOf(799));
        request.setStock(50);
        request.setCategory("Electronics");

        when(productService.createProduct(any()))
                .thenReturn(productResponse);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"));
    }

    @Test
    void createProduct_withInvalidPrice_returns400() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Wireless Mouse");
        request.setDescription("Ergonomic wireless mouse");
        request.setSku("WM-001");
        request.setPrice(BigDecimal.ZERO);
        request.setStock(50);
        request.setCategory("Electronics");

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_withNegativeStock_returns400() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Wireless Mouse");
        request.setDescription("Ergonomic wireless mouse");
        request.setSku("WM-001");
        request.setPrice(BigDecimal.valueOf(799));
        request.setStock(-1);
        request.setCategory("Electronics");

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}