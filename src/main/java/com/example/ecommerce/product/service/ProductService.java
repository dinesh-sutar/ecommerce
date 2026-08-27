package com.example.ecommerce.product.service;

import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.product.dto.CreateProductRequest;
import com.example.ecommerce.product.dto.ProductImageResponse;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductImageRepository;
import com.example.ecommerce.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public Page<ProductResponse> getProducts(
            String search,
            String category,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products;

        boolean hasSearch = search != null && !search.isBlank();

        boolean hasCategory = category != null && !category.isBlank();

        if (hasSearch && hasCategory) {

            products = productRepository
                    .findByNameContainingIgnoreCaseAndCategoryIgnoreCase(
                            search,
                            category,
                            pageable);

        } else if (hasSearch) {

            products = productRepository
                    .findByNameContainingIgnoreCase(
                            search,
                            pageable);

        } else if (hasCategory) {

            products = productRepository
                    .findByCategoryIgnoreCase(
                            category,
                            pageable);

        } else {

            products = productRepository.findAll(pageable);
        }

        return products.map(this::mapToResponse);
    }

    public ProductResponse getProductById(Long id) {

        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"));

        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(
            Product product) {

        List<ProductImageResponse> images = productImageRepository
                .findByProductIdOrderByIsPrimaryDesc(product.getId())
                .stream()
                .map(image -> ProductImageResponse.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .fileName(image.getFileName())
                        .isPrimary(image.getIsPrimary())
                        .build())
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .sku(product.getSku())
                .images(images)
                .build();
    }

    public ProductResponse createProduct(
            CreateProductRequest request) {

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .sku(request.getSku())
                .build();

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }
}