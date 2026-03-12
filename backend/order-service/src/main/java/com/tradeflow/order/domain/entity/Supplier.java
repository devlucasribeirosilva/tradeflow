package com.tradeflow.order.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
@Getter
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public Supplier(String name, String tenantId) {
        this.name = name;
        this.tenantId = tenantId;
    }
}