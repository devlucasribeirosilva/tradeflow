package com.tradeflow.order.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "buyers")
@Getter
@NoArgsConstructor
public class Buyer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public Buyer(String name, String email, String tenantId) {
        this.name = name;
        this.email = email;
        this.tenantId = tenantId;
    }
}