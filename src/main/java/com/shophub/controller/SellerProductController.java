package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.ApiResponse;
import com.shophub.dto.product.ProductRequest;
import com.shophub.dto.product.ProductResponse;
import com.shophub.security.UserPrincipal;
import com.shophub.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
@Tag(name = "Seller Products", description = "Seller product management (SELLER role required)")
public class SellerProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "Get all products owned by the authenticated seller")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMyProducts(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getSellerProducts(principal.getId())));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new product with optional image upload")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);
        // Manual validation workaround for @RequestPart JSON
        validateProductRequest(request);

        ProductResponse response = productService.createProduct(principal.getId(), request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);
        validateProductRequest(request);

        ProductResponse response = productService.updateProduct(principal.getId(), id, request, image);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        productService.deleteProduct(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    private void validateProductRequest(ProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new com.shophub.exception.BadRequestException("Product name is required");
        }
        if (request.getPrice() == null) {
            throw new com.shophub.exception.BadRequestException("Price is required");
        }
    }
}
