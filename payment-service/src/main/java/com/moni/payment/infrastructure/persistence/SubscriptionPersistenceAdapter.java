package com.moni.payment.infrastructure.persistence;

import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.moni.payment.domain.model.SubscriptionStatus.ACTIVE;
import static com.moni.payment.domain.model.SubscriptionStatus.CANCELLING;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;

    public SubscriptionPersistenceAdapter(
            SubscriptionJpaRepository subscriptionJpaRepository,
            SubscriptionHistoryRepository subscriptionHistoryRepository) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.subscriptionHistoryRepository = subscriptionHistoryRepository;
    }

    @Override
    public Subscription save(Subscription subscription) {
        return subscriptionJpaRepository.save(subscription);
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return subscriptionJpaRepository.findById(id);
    }

    @Override
    public Optional<Subscription> findActiveByUserId(UUID userId) {
        return subscriptionJpaRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }

    @Override
    public Optional<Subscription> findCurrentByUserId(UUID userId) {
        return subscriptionJpaRepository.findFirstByUserIdAndStatusIn(userId, List.of(ACTIVE, CANCELLING));
    }

    @Override
    public List<Subscription> findActiveSubscriptionsDueBefore(LocalDate date) {
        return subscriptionJpaRepository.findActiveSubscriptionsDueBefore(SubscriptionStatus.ACTIVE, date);
    }

    @Override
    public void saveHistory(SubscriptionHistory history) {
        subscriptionHistoryRepository.save(history);
    }
}
