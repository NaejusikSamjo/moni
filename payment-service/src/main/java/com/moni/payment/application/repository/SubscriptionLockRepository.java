package com.moni.payment.application.repository;

import java.time.Duration;
import java.util.UUID;

public interface SubscriptionLockRepository {

    boolean tryLock(UUID userId, Duration ttl);

    void unlock(UUID userId);
}
