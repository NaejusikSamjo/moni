package com.moni.payment.domain.port;

import com.moni.payment.domain.model.Subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadSubscriptionPort {

    Optional<Subscription> findById(UUID id);

    Optional<Subscription> findActiveByUserId(UUID userId);

    List<Subscription> findActiveSubscriptionsDueBefore(LocalDate date);
}
