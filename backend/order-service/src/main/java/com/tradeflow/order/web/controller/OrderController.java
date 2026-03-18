package com.tradeflow.order.web.controller;
import com.tradeflow.order.usecase.ListOrdersUseCase;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tradeflow.order.usecase.CreateOrderUseCase;
import com.tradeflow.order.usecase.GetOrderUseCase;
import com.tradeflow.order.web.dto.CreateOrderRequest;
import com.tradeflow.order.web.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;

    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String tenantId = (String) authentication.getDetails();
        OrderResponse response = createOrderUseCase.execute(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SUPPLIER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String tenantId = (String) authentication.getDetails();
        OrderResponse response = getOrderUseCase.execute(id, tenantId);
        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasAnyRole('BUYER', 'SUPPLIER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> listAll(Authentication authentication) {
        String tenantId = (String) authentication.getDetails();
        return ResponseEntity.ok(listOrdersUseCase.execute(tenantId));
    }
}