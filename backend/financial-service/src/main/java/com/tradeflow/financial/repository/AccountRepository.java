package com.tradeflow.financial.repository;

import com.tradeflow.financial.domain.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.ownerId = :ownerId AND a.ownerType = :ownerType")
    Optional<Account> findByOwnerIdAndOwnerTypeWithLock(
            @Param("ownerId") UUID ownerId,
            @Param("ownerType") String ownerType
    );

    Optional<Account> findByOwnerIdAndOwnerType(UUID ownerId, String ownerType);
}