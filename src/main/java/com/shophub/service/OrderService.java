package com.shophub.service;

import com.shophub.dto.event.OrderEvent;
import com.shophub.dto.order.OrderItemResponse;
import com.shophub.dto.order.OrderResponse;
import com.shophub.entity.*;
import com.shophub.exception.BadRequestException;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Transactional
    public OrderResponse createOrderFromCart(Long buyerId) {
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new BadRequestException("No cart found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty — nothing to order");
        }

        // Calculate total
        BigDecimal total = cartItems.stream()
                .map(ci -> ci.getProduct().getPrice()
                        .multiply(new BigDecimal(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create order
        Order order = Order.builder()
                .buyer(buyer)
                .status(Order.OrderStatus.PENDING)
                .totalPrice(total)
                .build();
        order = orderRepository.save(order);

        // Create order items
        final Order savedOrder = order;
        List<OrderItem> orderItems = cartItems.stream().map(ci -> OrderItem.builder()
                .order(savedOrder)
                .product(ci.getProduct())
                .seller(ci.getProduct().getSeller())
                .quantity(ci.getQuantity())
                .price(ci.getProduct().getPrice())
                .build()).toList();
        orderItemRepository.saveAll(orderItems);

        // Clear cart
        cartItemRepository.deleteAll(cartItems);

        // Build response
        OrderResponse response = buildOrderResponse(order, orderItems);

        // Publish local async order event
        publishOrderEvent(buyer, order, orderItems);

        return response;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getBuyerOrders(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(o -> {
                    List<OrderItem> items = orderItemRepository.findByOrderId(o.getId());
                    return buildOrderResponse(o, items);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getSellerOrders(Long sellerId) {
        List<OrderItem> sellerItems = orderItemRepository
                .findBySellerIdOrderByOrder_CreatedAtDesc(sellerId);

        // Group by order
        return sellerItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        si -> si.getOrder().getId()))
                .values().stream()
                .map(items -> buildOrderResponse(items.get(0).getOrder(), items))
                .toList();
    }

    private OrderResponse buildOrderResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream().map(i ->
                OrderItemResponse.builder()
                        .orderItemId(i.getId())
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .imageUrl(i.getProduct().getImageUrl())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .sellerId(i.getSeller().getId())
                        .sellerName(i.getSeller().getUsername())
                        .build()
        ).toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private void publishOrderEvent(User buyer, Order order, List<OrderItem> orderItems) {
        try {
            List<OrderEvent.OrderEventItem> eventItems = orderItems.stream().map(i ->
                    OrderEvent.OrderEventItem.builder()
                            .productId(i.getProduct().getId())
                            .productName(i.getProduct().getName())
                            .sellerId(i.getSeller().getId())
                            .quantity(i.getQuantity())
                            .price(i.getPrice())
                            .build()
            ).toList();

            OrderEvent event = OrderEvent.builder()
                    .orderId(order.getId())
                    .buyerId(buyer.getId())
                    .buyerEmail(buyer.getEmail())
                    .totalPrice(order.getTotalPrice())
                    .status(order.getStatus().name())
                    .createdAt(order.getCreatedAt())
                    .items(eventItems)
                    .build();

            orderEventPublisher.publishOrderCreated(event);
        } catch (Exception e) {
            log.error("Failed to publish local order event for orderId={}: {}", order.getId(), e.getMessage());
        }
    }
}
