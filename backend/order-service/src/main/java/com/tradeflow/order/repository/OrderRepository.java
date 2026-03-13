package com.tradeflow.order.repository;

import com.tradeflow.order.domain.entity.Order;
import com.tradeflow.order.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdAndTenantId(UUID id, String tenantId);

    Page<Order> findAllByTenantId(String tenantId, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.idempotencyKey = :key")
    boolean existsByIdempotencyKey(@Param("key") String idempotencyKey);
}