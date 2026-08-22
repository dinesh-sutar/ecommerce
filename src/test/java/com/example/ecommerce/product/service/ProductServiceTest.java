package com.example.ecommerce.product.service;

import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.product.dto.CreateProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

        @Mock
        private ProductRepository productRepository;

        @InjectMocks
        private ProductService productService;

        private Product product;

        @BeforeEach
        void setUp() {

                product = Product.builder()
                                .id(1L)
                                .name("Laptop")
                                .description("Gaming Laptop")
                                .sku("LAP-001")
                                .price(new BigDecimal("50000.00"))
                                .stock(10)
                                .category("Electronics")
                                .active(true)
                                .build();
        }

        @Test
        void shouldGetProductByIdSuccessfully() {

                when(productRepository.findById(1L))
                                .thenReturn(Optional.of(product));

                ProductResponse response = productService.getProductById(1L);

                assertNotNull(response);

                assertEquals(1L, response.getId());
                assertEquals("Laptop", response.getName());
                assertEquals("Gaming Laptop", response.getDescription());
                assertEquals(new BigDecimal("50000.00"), response.getPrice());
                assertEquals(10, response.getStock());
                assertEquals("Electronics", response.getCategory());

                verify(productRepository)
                                .findById(1L);
        }

        @Test
        void shouldThrowExceptionWhenProductNotFound() {

                when(productRepository.findById(1L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> productService.getProductById(1L));

                verify(productRepository)
                                .findById(1L);
        }

        @Test
        void shouldCreateProductSuccessfully() {

                CreateProductRequest request = new CreateProductRequest();

                request.setName("Laptop");
                request.setDescription("Gaming Laptop");
                request.setSku("LAP-001");
                request.setPrice(new BigDecimal("50000.00"));
                request.setStock(10);
                request.setCategory("Electronics");

                when(productRepository.save(any(Product.class)))
                                .thenReturn(product);

                ProductResponse response = productService.createProduct(request);

                assertNotNull(response);

                assertEquals(1L, response.getId());
                assertEquals("Laptop", response.getName());
                assertEquals(new BigDecimal("50000.00"), response.getPrice());

                verify(productRepository)
                                .save(any(Product.class));
        }

        @Test
        void shouldGetAllProducts() {

                Page<Product> productPage = new PageImpl<>(List.of(product));

                when(productRepository.findAll(any(PageRequest.class)))
                                .thenReturn(productPage);

                Page<ProductResponse> response = productService.getProducts(
                                null,
                                null,
                                0,
                                10);

                assertEquals(1, response.getTotalElements());

                assertEquals(
                                "Laptop",
                                response.getContent().get(0).getName());

                verify(productRepository)
                                .findAll(any(PageRequest.class));
        }

        @Test
        void shouldGetProductsBySearchAndCategory() {

                Page<Product> productPage = new PageImpl<>(List.of(product));

                when(productRepository
                                .findByNameContainingIgnoreCaseAndCategoryIgnoreCase(
                                                eq("laptop"),
                                                eq("Electronics"),
                                                any(PageRequest.class)))
                                .thenReturn(productPage);

                Page<ProductResponse> response = productService.getProducts(
                                "laptop",
                                "Electronics",
                                0,
                                10);

                assertEquals(1, response.getTotalElements());
                assertEquals(
                                "Laptop",
                                response.getContent().get(0).getName());

                verify(productRepository)
                                .findByNameContainingIgnoreCaseAndCategoryIgnoreCase(
                                                eq("laptop"),
                                                eq("Electronics"),
                                                any(PageRequest.class));
        }

        @Test
        void shouldGetProductsBySearch() {

                Page<Product> productPage = new PageImpl<>(List.of(product));

                when(productRepository
                                .findByNameContainingIgnoreCase(
                                                eq("laptop"),
                                                any(PageRequest.class)))
                                .thenReturn(productPage);

                Page<ProductResponse> response = productService.getProducts(
                                "laptop",
                                null,
                                0,
                                10);

                assertEquals(1, response.getTotalElements());
                assertEquals(
                                "Laptop",
                                response.getContent().get(0).getName());

                verify(productRepository)
                                .findByNameContainingIgnoreCase(
                                                eq("laptop"),
                                                any(PageRequest.class));
        }

        @Test
        void shouldGetProductsByCategory() {

                Page<Product> productPage = new PageImpl<>(List.of(product));

                when(productRepository
                                .findByCategoryIgnoreCase(
                                                eq("Electronics"),
                                                any(PageRequest.class)))
                                .thenReturn(productPage);

                Page<ProductResponse> response = productService.getProducts(
                                null,
                                "Electronics",
                                0,
                                10);

                assertEquals(1, response.getTotalElements());
                assertEquals(
                                "Laptop",
                                response.getContent().get(0).getName());

                verify(productRepository)
                                .findByCategoryIgnoreCase(
                                                eq("Electronics"),
                                                any(PageRequest.class));
        }
}