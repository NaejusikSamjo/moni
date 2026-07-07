package com.moni.payment.application.scheduler;

import com.moni.payment.application.service.command.SubscriptionCommandService;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteCancellationScheduler {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionCommandService subscriptionCommandService;

    @Scheduled(cron = "0 0 0 * * *")
    public void completeExpiredCancellations() {
        List<Subscription> targets = subscriptionJpaRepository
                .findActiveSubscriptionsDueBefore(SubscriptionStatus.CANCELLING, LocalDate.now());

        log.info("CANCELLING → CANCELLED 처리 대상: {}건", targets.size());

        for (Subscription subscription : targets) {
            try {
                subscriptionCommandService.completeCancellation(subscription.getId());
            } catch (Exception e) {
                log.error("구독 CANCELLED 전환 실패: subscriptionId={}", subscription.getId(), e);
            }
        }
    }
}
