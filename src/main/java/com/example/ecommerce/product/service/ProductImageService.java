package com.example.ecommerce.product.service;

import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.product.dto.ProductImageResponse;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.entity.ProductImage;
import com.example.ecommerce.product.repository.ProductImageRepository;
import com.example.ecommerce.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    private static final String UPLOAD_DIRECTORY = "uploads/products";

    public List<ProductImageResponse> uploadImages(
            Long productId,
            List<MultipartFile> files) {

        // 1. Check product exists
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"));

        // 2. Validate files
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one image is required");
        }

        // 3. Create product-specific directory
        Path productDirectory = Paths.get(
                UPLOAD_DIRECTORY,
                productId.toString());

        try {

            Files.createDirectories(productDirectory);

            List<ProductImageResponse> responses = new ArrayList<>();

            // Check whether product already has images
            boolean hasExistingImages = productImageRepository
                    .countByProductId(productId) > 0;

            boolean firstImage = !hasExistingImages;

            // 4. Process each image
            for (MultipartFile file : files) {

                if (file == null || file.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Image file cannot be empty");
                }

                String originalFileName = file.getOriginalFilename();

                System.out.println("Original filename: " + originalFileName);

                if (originalFileName == null
                        || originalFileName.isBlank()) {

                    throw new IllegalArgumentException(
                            "Invalid image filename");
                }

                String fileExtension = getFileExtension(originalFileName)
                        .toLowerCase();

                boolean validExtension = fileExtension.equals(".jpg")
                        || fileExtension.equals(".jpeg")
                        || fileExtension.equals(".png")
                        || fileExtension.equals(".webp");

                System.out.println("File extension: " + fileExtension);

                if (!validExtension) {
                    throw new IllegalArgumentException(
                            "Only JPG, JPEG, PNG and WEBP image files are allowed");
                }

                String uniqueFileName = UUID.randomUUID() + fileExtension;

                Path filePath = productDirectory.resolve(uniqueFileName);

                Files.copy(
                        file.getInputStream(),
                        filePath);

                String imageUrl = "/uploads/products/"
                        + productId
                        + "/"
                        + uniqueFileName;

                boolean isPrimary = firstImage;

                firstImage = false;

                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrl)
                        .fileName(uniqueFileName)
                        .isPrimary(isPrimary)
                        .build();

                ProductImage savedImage = productImageRepository.save(productImage);

                responses.add(mapToResponse(savedImage));
            }

            return responses;

        } catch (IOException ex) {

            throw new RuntimeException(
                    "Failed to upload images",
                    ex);
        }
    }

    private String getFileExtension(
            String fileName) {

        int lastDotIndex = fileName.lastIndexOf(".");

        if (lastDotIndex == -1) {
            return "";
        }

        return fileName.substring(
                lastDotIndex);
    }

    private ProductImageResponse mapToResponse(
            ProductImage image) {

        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .fileName(image.getFileName())
                .isPrimary(image.getIsPrimary())
                .build();
    }
}