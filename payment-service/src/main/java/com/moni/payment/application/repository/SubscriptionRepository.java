package com.moni.payment.application.repository;

import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {

    Subscription save(Subscription subscription);

    Optional<Subscription> findById(UUID id);

    Optional<Subscription> findActiveByUserId(UUID userId);

    Optional<Subscription> findCurrentByUserId(UUID userId);

    List<Subscription> findActiveSubscriptionsDueBefore(LocalDate date);

    void saveHistory(SubscriptionHistory history);
}
