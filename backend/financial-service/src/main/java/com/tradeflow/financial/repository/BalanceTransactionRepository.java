package com.tradeflow.financial.repository;

import com.tradeflow.financial.domain.entity.BalanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BalanceTransactionRepository extends JpaRepository<BalanceTransaction, UUID> {
}