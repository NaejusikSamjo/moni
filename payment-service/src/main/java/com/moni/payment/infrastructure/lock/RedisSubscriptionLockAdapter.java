package com.moni.payment.infrastructure.lock;

import com.moni.payment.application.repository.SubscriptionLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisSubscriptionLockAdapter implements SubscriptionLockRepository {

    private static final String LOCK_PREFIX = "subscription:lock:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryLock(UUID userId, Duration ttl) {
        String key = LOCK_PREFIX + userId;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void unlock(UUID userId) {
        stringRedisTemplate.delete(LOCK_PREFIX + userId);
    }
}
