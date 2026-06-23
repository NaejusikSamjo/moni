package com.moni.payment.infrastructure.persistence;

import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.domain.port.LoadSubscriptionPort;
import com.moni.payment.domain.port.SaveSubscriptionHistoryPort;
import com.moni.payment.domain.port.SaveSubscriptionPort;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryJpaRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionPersistenceAdapter
        implements SaveSubscriptionPort, LoadSubscriptionPort, SaveSubscriptionHistoryPort {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionHistoryJpaRepository subscriptionHistoryJpaRepository;

    public SubscriptionPersistenceAdapter(
            SubscriptionJpaRepository subscriptionJpaRepository,
            SubscriptionHistoryJpaRepository subscriptionHistoryJpaRepository) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.subscriptionHistoryJpaRepository = subscriptionHistoryJpaRepository;
    }

    @Override
    @Transactional
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = SubscriptionJpaEntity.fromDomain(subscription);
        return subscriptionJpaRepository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findById(UUID id) {
        return subscriptionJpaRepository.findById(id)
                .map(SubscriptionJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findActiveByUserId(UUID userId) {
        return subscriptionJpaRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(SubscriptionJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> findActiveSubscriptionsDueBefore(LocalDate date) {
        return subscriptionJpaRepository.findActiveSubscriptionsDueBefore(date)
                .stream()
                .map(SubscriptionJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void save(SubscriptionHistory history) {
        subscriptionJpaRepository.findById(history.getSubscriptionId())
                .ifPresent(subscriptionEntity -> {
                    SubscriptionHistoryJpaEntity historyEntity =
                            SubscriptionHistoryJpaEntity.fromDomain(history, subscriptionEntity);
                    subscriptionHistoryJpaRepository.save(historyEntity);
                });
    }
}
