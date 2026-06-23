package com.moni.payment.infrastructure.persistence;

import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.domain.port.LoadSubscriptionPort;
import com.moni.payment.domain.port.SaveSubscriptionHistoryPort;
import com.moni.payment.domain.port.SaveSubscriptionPort;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionPersistenceAdapter
        implements SaveSubscriptionPort, LoadSubscriptionPort, SaveSubscriptionHistoryPort {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;

    public SubscriptionPersistenceAdapter(
            SubscriptionRepository subscriptionRepository,
            SubscriptionHistoryRepository subscriptionHistoryRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionHistoryRepository = subscriptionHistoryRepository;
    }

    @Override
    @Transactional
    public Subscription save(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findById(UUID id) {
        return subscriptionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findActiveByUserId(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> findActiveSubscriptionsDueBefore(LocalDate date) {
        return subscriptionRepository.findActiveSubscriptionsDueBefore(SubscriptionStatus.ACTIVE, date);
    }

    @Override
    @Transactional
    public void save(SubscriptionHistory history) {
        subscriptionHistoryRepository.save(history);
    }
}
