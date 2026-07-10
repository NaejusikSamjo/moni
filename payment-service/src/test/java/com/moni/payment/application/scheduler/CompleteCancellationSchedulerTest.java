package com.moni.payment.application.scheduler;

import com.moni.payment.application.service.command.SubscriptionCommandService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompleteCancellationScheduler 테스트")
class CompleteCancellationSchedulerTest {

    @Mock private SubscriptionJpaRepository subscriptionJpaRepository;
    @Mock private SubscriptionCommandService subscriptionCommandService;

    @InjectMocks
    private CompleteCancellationScheduler completeCancellationScheduler;

    private Subscription cancellingSubscription() {
        Subscription s = Subscription.create(java.util.UUID.randomUUID());
        s.activate(BillingKey.of("billing-key-001"), Money.of(new BigDecimal("9900")));
        s.cancel("테스트 취소");
        return s;
    }

    @Nested
    @DisplayName("completeExpiredCancellations()")
    class CompleteExpiredCancellations {

        @Test
        void 대상_구독이_없으면_completeCancellation을_호출하지_않는다() {
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.CANCELLING), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());

            completeCancellationScheduler.completeExpiredCancellations();

            then(subscriptionCommandService).should(never()).completeCancellation(any());
        }

        @Test
        void CANCELLING_만료_구독에_대해_completeCancellation을_호출한다() {
            Subscription subscription = cancellingSubscription();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.CANCELLING), any(LocalDate.class)))
                    .willReturn(List.of(subscription));

            completeCancellationScheduler.completeExpiredCancellations();

            then(subscriptionCommandService).should().completeCancellation(subscription.getId());
        }

        @Test
        void completeCancellation_예외_발생_시_다음_구독을_계속_처리한다() {
            Subscription first = cancellingSubscription();
            Subscription second = cancellingSubscription();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.CANCELLING), any(LocalDate.class)))
                    .willReturn(List.of(first, second));
            willThrow(new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND))
                    .given(subscriptionCommandService).completeCancellation(first.getId());

            completeCancellationScheduler.completeExpiredCancellations();

            then(subscriptionCommandService).should().completeCancellation(second.getId());
        }

        @Test
        void 복수_대상에_대해_각각_completeCancellation을_호출한다() {
            Subscription first = cancellingSubscription();
            Subscription second = cancellingSubscription();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.CANCELLING), any(LocalDate.class)))
                    .willReturn(List.of(first, second));

            completeCancellationScheduler.completeExpiredCancellations();

            then(subscriptionCommandService).should().completeCancellation(first.getId());
            then(subscriptionCommandService).should().completeCancellation(second.getId());
        }
    }
}
