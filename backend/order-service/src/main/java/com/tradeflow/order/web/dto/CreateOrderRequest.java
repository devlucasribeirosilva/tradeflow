package com.tradeflow.order.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "Buyer ID is required")
        UUID buyerId,

        @NotNull(message = "Supplier ID is required")
        UUID supplierId,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(

            @NotBlank(message = "Product name is required")
            String productName,

            @NotNull(message = "Quantity is required")
            Integer quantity,

            @NotNull(message = "Unit price is required")
            Double unitPrice,

            @NotBlank(message = "Currency is required")
            String currency
    ) {}
}