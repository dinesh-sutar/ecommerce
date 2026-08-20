package com.example.ecommerce.product.service;

import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found"));

        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(
            Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .build();
    }
}