package com.tradeflow.order.usecase;

import com.tradeflow.order.repository.OrderRepository;
import com.tradeflow.order.web.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListOrdersUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<OrderResponse> execute(String tenantId) {
        return orderRepository.findAllByTenantId(tenantId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}