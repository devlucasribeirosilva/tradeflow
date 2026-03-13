package com.tradeflow.order.usecase;

import com.tradeflow.order.domain.entity.Buyer;
import com.tradeflow.order.domain.entity.Order;
import com.tradeflow.order.domain.entity.OrderItem;
import com.tradeflow.order.domain.entity.Supplier;
import com.tradeflow.order.domain.valueobject.Money;
import com.tradeflow.order.repository.BuyerRepository;
import com.tradeflow.order.repository.OrderRepository;
import com.tradeflow.order.repository.SupplierRepository;
import com.tradeflow.order.web.dto.CreateOrderRequest;
import com.tradeflow.order.web.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final BuyerRepository buyerRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public OrderResponse execute(CreateOrderRequest request, String tenantId) {

        // Idempotency check — retorna a ordem existente se já foi processada
        var existing = orderRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key: {}", request.idempotencyKey());
            return OrderResponse.from(existing.get());
        }

        Buyer buyer = buyerRepository.findByIdAndTenantId(request.buyerId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found or unauthorized"));

        Supplier supplier = supplierRepository.findByIdAndTenantId(request.supplierId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found or unauthorized"));

        Order order = new Order(buyer, supplier, request.idempotencyKey(), tenantId);

        request.items().forEach(itemRequest -> {
            Money unitPrice = new Money(
                    BigDecimal.valueOf(itemRequest.unitPrice()),
                    itemRequest.currency()
            );
            order.addItem(new OrderItem(
                    itemRequest.productName(),
                    itemRequest.quantity(),
                    unitPrice
            ));
        });

        Order saved = orderRepository.save(order);
        log.info("Order created with id: {} for tenant: {}", saved.getId(), tenantId);

        return OrderResponse.from(saved);
    }
}