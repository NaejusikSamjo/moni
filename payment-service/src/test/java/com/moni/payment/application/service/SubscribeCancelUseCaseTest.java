package com.moni.payment.application.service;

import com.moni.payment.application.dto.command.CancelSubscriptionResult;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeCancelUseCase 테스트")
class SubscribeCancelUseCaseTest {

    @Mock private SubscriptionJpaRepository subscriptionJpaRepository;
    @Mock private SubscriptionHistoryRepository subscriptionHistoryRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private SubscribeCancelUseCase subscribeCancelUseCase;

    private static final UUID USER_ID = UUID.randomUUID();

    private Subscription activeSubscription() {
        Subscription s = Subscription.create(USER_ID);
        s.activate(BillingKey.of("billing-key-001"), Money.of(new BigDecimal("9900")));
        return s;
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        void ACTIVE_구독이_없으면_SUB_002_예외가_발생한다() {
            given(subscriptionJpaRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> subscribeCancelUseCase.execute(USER_ID))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
        }

        @Test
        void ACTIVE_구독이_있으면_CANCELLING으로_전이하고_저장한다() {
            Subscription subscription = activeSubscription();
            given(subscriptionJpaRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                    .willReturn(Optional.of(subscription));
            given(subscriptionJpaRepository.save(any())).willReturn(subscription);

            CancelSubscriptionResult result = subscribeCancelUseCase.execute(USER_ID);

            assertThat(result.status()).isEqualTo(SubscriptionStatus.CANCELLING);
            assertThat(result.subscriptionId()).isEqualTo(subscription.getId());
        }

        @Test
        void 해지_후_구독_이력이_저장된다() {
            Subscription subscription = activeSubscription();
            given(subscriptionJpaRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                    .willReturn(Optional.of(subscription));
            given(subscriptionJpaRepository.save(any())).willReturn(subscription);

            subscribeCancelUseCase.execute(USER_ID);

            then(subscriptionHistoryRepository).should().saveAll(any());
        }

        @Test
        void 해지_후_도메인_이벤트가_발행된다() {
            Subscription subscription = activeSubscription();
            given(subscriptionJpaRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                    .willReturn(Optional.of(subscription));
            given(subscriptionJpaRepository.save(any())).willReturn(subscription);

            subscribeCancelUseCase.execute(USER_ID);

            then(applicationEventPublisher).should(org.mockito.Mockito.atLeastOnce()).publishEvent((Object) any());
        }
    }
}
