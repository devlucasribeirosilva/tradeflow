package com.tradeflow.financial.service;

import com.tradeflow.financial.domain.entity.Account;
import com.tradeflow.financial.domain.entity.BalanceTransaction;
import com.tradeflow.financial.repository.AccountRepository;
import com.tradeflow.financial.repository.BalanceTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final AccountRepository accountRepository;
    private final BalanceTransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String BALANCE_CACHE_KEY = "balance:%s";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    public BigDecimal getBalance(UUID ownerId, String ownerType) {
        String cacheKey = String.format(BALANCE_CACHE_KEY, ownerId);
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.debug("Cache hit for balance: {}", ownerId);
            return new BigDecimal(cached);
        }

        log.debug("Cache miss for balance: {}", ownerId);
        Account account = accountRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + ownerId));

        redisTemplate.opsForValue().set(cacheKey, account.getBalance().toString(), CACHE_TTL);
        return account.getBalance();
    }

    @Transactional
    public void reserveBalance(UUID ownerId, String ownerType, UUID orderId, BigDecimal amount) {
        Account account = accountRepository
                .findByOwnerIdAndOwnerTypeWithLock(ownerId, ownerType)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + ownerId));

        BigDecimal balanceBefore = account.getBalance();
        account.debit(amount);

        transactionRepository.save(new BalanceTransaction(
                account.getId(), orderId, "DEBIT",
                amount, balanceBefore, account.getBalance()
        ));

        redisTemplate.delete(String.format(BALANCE_CACHE_KEY, ownerId));

        log.info("Balance reserved: {} for order: {} — new balance: {}",
                amount, orderId, account.getBalance());
    }

    @Transactional
    public void creditBalance(UUID ownerId, String ownerType, UUID orderId,
                              BigDecimal amount, String tenantId) {
        Account account = accountRepository
                .findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseGet(() -> accountRepository.save(
                        new Account(ownerId, ownerType, tenantId, "BRL")
                ));

        BigDecimal balanceBefore = account.getBalance();
        account.credit(amount);

        transactionRepository.save(new BalanceTransaction(
                account.getId(), orderId, "CREDIT",
                amount, balanceBefore, account.getBalance()
        ));

        redisTemplate.delete(String.format(BALANCE_CACHE_KEY, ownerId));

        log.info("Balance credited: {} for owner: {} — new balance: {}",
                amount, ownerId, account.getBalance());
    }
}