package com.shophub.controller;

import com.shophub.dto.ApiResponse;
import com.shophub.dto.order.OrderResponse;
import com.shophub.security.UserPrincipal;
import com.shophub.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
@Tag(name = "Orders (Seller)", description = "Seller received orders (SELLER role required)")
public class SellerOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders containing the seller's products")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getSellerOrders(principal.getId())));
    }
}
