package com.tradeflow.order.web.controller;

import com.tradeflow.order.usecase.CreateOrderUseCase;
import com.tradeflow.order.usecase.GetOrderUseCase;
import com.tradeflow.order.web.dto.CreateOrderRequest;
import com.tradeflow.order.web.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        OrderResponse response = createOrderUseCase.execute(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        OrderResponse response = getOrderUseCase.execute(id, tenantId);
        return ResponseEntity.ok(response);
    }
}