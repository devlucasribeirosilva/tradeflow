package com.tradeflow.order.usecase;
import com.tradeflow.order.domain.entity.Buyer;
import com.tradeflow.order.domain.entity.Order;
import com.tradeflow.order.domain.entity.Supplier;
import com.tradeflow.order.messaging.OrderEventPublisher;
import com.tradeflow.order.repository.BuyerRepository;
import com.tradeflow.order.repository.OrderRepository;
import com.tradeflow.order.repository.SupplierRepository;
import com.tradeflow.order.web.dto.CreateOrderRequest;
import com.tradeflow.order.web.dto.OrderResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private BuyerRepository buyerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private OrderEventPublisher eventPublisher;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Timer timer;

    @InjectMocks
    private CreateOrderUseCase useCase;

    private Buyer buyer;
    private Supplier supplier;
    private final String tenantId = "tenant-001";

    @BeforeEach
    void setUp() {
        buyer = new Buyer("Acme Corp", "acme@test.com", tenantId);
        supplier = new Supplier("TechSupplies", tenantId);

        when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(timer);
        when(timer.record(any(java.util.function.Supplier.class))).thenAnswer(inv -> {
            java.util.function.Supplier<?> s = inv.getArgument(0);
            return s.get();
        });
        when(meterRegistry.counter(anyString(), any(String[].class)))
                .thenReturn(new SimpleMeterRegistry().counter("test"));
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrderSuccessfully() {
        var request = buildRequest("idem-key-001");

        when(orderRepository.findByIdempotencyKey("idem-key-001")).thenReturn(Optional.empty());
        when(buyerRepository.findByIdAndTenantId(any(), eq(tenantId))).thenReturn(Optional.of(buyer));
        when(supplierRepository.findByIdAndTenantId(any(), eq(tenantId))).thenReturn(Optional.of(supplier));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = useCase.execute(request, tenantId);

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Should return existing order on duplicate idempotency key")
    void shouldReturnExistingOrderOnDuplicateKey() {
        var request = buildRequest("idem-key-dup");
        var existingOrder = new Order(buyer, supplier, "idem-key-dup", tenantId);

        when(orderRepository.findByIdempotencyKey("idem-key-dup")).thenReturn(Optional.of(existingOrder));

        useCase.execute(request, tenantId);
        useCase.execute(request, tenantId);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when buyer not found")
    void shouldThrowWhenBuyerNotFound() {
        var request = buildRequest("idem-key-002");

        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(buyerRepository.findByIdAndTenantId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request, tenantId))
                .isInstanceOf(Exception.class);
    }

    private CreateOrderRequest buildRequest(String idempotencyKey) {
        return new CreateOrderRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                idempotencyKey,
                List.of(new CreateOrderRequest.OrderItemRequest("Notebook", 2, 3500.00, "BRL"))
        );
    }
}