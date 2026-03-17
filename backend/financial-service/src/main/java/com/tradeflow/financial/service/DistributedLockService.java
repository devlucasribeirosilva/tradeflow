package com.tradeflow.financial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "lock:account:%s";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 100;

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    public String acquireLock(UUID ownerId) {
        String lockKey = String.format(LOCK_KEY, ownerId);
        String lockValue = UUID.randomUUID().toString();

        for (int i = 0; i < MAX_RETRIES; i++) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, LOCK_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Lock acquired for account: {}", ownerId);
                return lockValue;
            }

            log.debug("Lock busy for account: {}, retry {}/{}", ownerId, i + 1, MAX_RETRIES);
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for lock");
            }
        }

        throw new IllegalStateException("Could not acquire lock for account: " + ownerId);
    }

    public void releaseLock(UUID ownerId, String lockValue) {
        String lockKey = String.format(LOCK_KEY, ownerId);
        redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                        UNLOCK_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                lockValue
        );
        log.debug("Lock released for account: {}", ownerId);
    }
}