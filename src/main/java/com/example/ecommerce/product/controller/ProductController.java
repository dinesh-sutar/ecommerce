package com.example.ecommerce.product.controller;

import com.example.ecommerce.product.dto.CreateProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

        private final ProductService productService;

        @GetMapping
        public ResponseEntity<Page<ProductResponse>> getProducts(

                        @RequestParam(required = false) String search,

                        @RequestParam(required = false) String category,

                        @RequestParam(defaultValue = "0") int page,

                        @RequestParam(defaultValue = "10") int size) {

                return ResponseEntity.ok(
                                productService.getProducts(
                                                search,
                                                category,
                                                page,
                                                size));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ProductResponse> getProductById(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                productService.getProductById(id));
        }

        @PostMapping
        public ResponseEntity<ProductResponse> createProduct(
                        @Valid @RequestBody CreateProductRequest request) {

                ProductResponse response = productService.createProduct(request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }
}