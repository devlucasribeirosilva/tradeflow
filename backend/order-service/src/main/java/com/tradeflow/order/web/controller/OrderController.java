package com.tradeflow.order.web.controller;
import org.springframework.security.core.Authentication;

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
            Authentication authentication
    ) {
        String tenantId = (String) authentication.getDetails();
        OrderResponse response = createOrderUseCase.execute(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        String tenantId = (String) authentication.getDetails();
        OrderResponse response = getOrderUseCase.execute(id, tenantId);
        return ResponseEntity.ok(response);
    }
}