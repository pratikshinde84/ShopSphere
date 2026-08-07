package com.shophub.service;

import com.shophub.dto.cart.CartItemRequest;
import com.shophub.dto.cart.CartItemResponse;
import com.shophub.dto.cart.CartItemUpdateRequest;
import com.shophub.dto.cart.CartResponse;
import com.shophub.entity.Cart;
import com.shophub.entity.CartItem;
import com.shophub.entity.Product;
import com.shophub.entity.User;
import com.shophub.exception.BadRequestException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.repository.CartItemRepository;
import com.shophub.repository.CartRepository;
import com.shophub.repository.ProductRepository;
import com.shophub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long buyerId) {
        Cart cart = getOrCreateCart(buyerId);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long buyerId, CartItemRequest request) {
        Cart cart = getOrCreateCart(buyerId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + product.getQuantity());
        }

        // Merge if already in cart
        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(item);
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long buyerId, Long itemId, CartItemUpdateRequest request) {
        Cart cart = getOrCreateCart(buyerId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to your cart");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long buyerId, Long itemId) {
        Cart cart = getOrCreateCart(buyerId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to your cart");
        }

        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    public Cart getOrCreateCart(Long buyerId) {
        return cartRepository.findByBuyerId(buyerId)
                .orElseGet(() -> {
                    User buyer = userRepository.findById(buyerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    return cartRepository.save(Cart.builder().buyer(buyer).build());
                });
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = items.stream().map(this::mapItem).toList();
        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .total(total)
                .itemCount(items.size())
                .build();
    }

    private CartItemResponse mapItem(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(new BigDecimal(item.getQuantity()));
        return CartItemResponse.builder()
                .itemId(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .imageUrl(item.getProduct().getImageUrl())
                .price(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
