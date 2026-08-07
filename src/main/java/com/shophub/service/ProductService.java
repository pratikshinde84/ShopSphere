package com.shophub.service;

import com.shophub.dto.product.ProductRequest;
import com.shophub.dto.product.ProductResponse;
import com.shophub.entity.Product;
import com.shophub.entity.User;
import com.shophub.exception.BadRequestException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.exception.UnauthorizedException;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.UserRepository;
import com.shophub.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Cacheable(value = "products", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Cacheable(value = "products", key = "'id:' + #id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Cacheable(value = "products", key = "'search:' + #query + ':' + #pageable.pageNumber")
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productRepository.searchByQuery(query, pageable).map(this::mapToResponse);
    }

    public List<ProductResponse> getSellerProducts(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(Long sellerId, ProductRequest request, MultipartFile image) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = storageService.uploadFile(image);
        }

        Product product = Product.builder()
                .seller(seller)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .imageUrl(imageUrl)
                .build();

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", key = "'id:' + #productId"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public ProductResponse updateProduct(Long sellerId, Long productId,
                                          ProductRequest request, MultipartFile image) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("You can only edit your own products");
        }

        if (image != null && !image.isEmpty()) {
            if (product.getImageUrl() != null) {
                storageService.deleteFile(product.getImageUrl());
            }
            product.setImageUrl(storageService.uploadFile(image));
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory());

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", key = "'id:' + #productId"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public void deleteProduct(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new UnauthorizedException("You can only delete your own products");
        }

        if (product.getImageUrl() != null) {
            storageService.deleteFile(product.getImageUrl());
        }

        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .sellerId(p.getSeller().getId())
                .sellerName(p.getSeller().getUsername())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .category(p.getCategory())
                .imageUrl(p.getImageUrl())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getCreatedAt())
                .build();
    }
}
