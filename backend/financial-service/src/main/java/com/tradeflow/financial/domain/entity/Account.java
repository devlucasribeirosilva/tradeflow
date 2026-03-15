package com.tradeflow.financial.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", schema = "financial")
@Getter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "owner_type", nullable = false)
    private String ownerType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Account(UUID ownerId, String ownerType, String tenantId, String currency) {
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.tenantId = tenantId;
        this.balance = BigDecimal.ZERO;
        this.currency = currency;
        this.createdAt = LocalDateTime.now();
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Credit amount must be positive");
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Debit amount must be positive");
        if (this.balance.compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient balance");
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
}