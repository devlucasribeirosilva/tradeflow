package com.tradeflow.order.usecase;
import com.tradeflow.order.domain.exception.OrderNotFoundException;
import com.tradeflow.order.repository.OrderRepository;
import com.tradeflow.order.web.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderResponse execute(UUID id, String tenantId) {
        return orderRepository.findByIdAndTenantId(id, tenantId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}