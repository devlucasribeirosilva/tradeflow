package com.tradeflow.order.web.dto;

import com.tradeflow.order.domain.entity.Order;
import com.tradeflow.order.domain.entity.OrderItem;
import com.tradeflow.order.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String idempotencyKey,
        UUID buyerId,
        UUID supplierId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public record OrderItemResponse(
            UUID id,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {}

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getIdempotencyKey(),
                order.getBuyer().getId(),
                order.getSupplier().getId(),
                order.getStatus(),
                order.getTotalAmount() != null ? order.getTotalAmount().getAmount() : null,
                order.getTotalAmount() != null ? order.getTotalAmount().getCurrency() : null,
                order.getItems().stream().map(OrderResponse::mapItem).toList(),
                order.getCreatedAt()
        );
    }

    private static OrderItemResponse mapItem(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice().getAmount(),
                item.totalPrice().getAmount()
        );
    }
}