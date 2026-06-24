package com.moni.payment.application.service;

import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionQueryService")
class SubscriptionQueryServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionQueryService subscriptionQueryService;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        subscriptionQueryService = new SubscriptionQueryService(subscriptionRepository);
    }

    private Subscription activeSubscription() {
        Instant now = Instant.now();
        return Subscription.reconstitute(
                UUID.randomUUID(), USER_ID, BillingKey.of("bk-001"),
                SubscriptionStatus.ACTIVE, LocalDate.now().plusMonths(1),
                now, now, 1L, java.util.List.of());
    }

    @Nested
    @DisplayName("getSubscriptionStatus()")
    class GetSubscriptionStatus {

        @Test
        @DisplayName("ACTIVE 구독이 존재하면 반환한다")
        void returnsActiveSubscription() {
            given(subscriptionRepository.findActiveByUserId(USER_ID))
                    .willReturn(Optional.of(activeSubscription()));

            Subscription result = subscriptionQueryService.getSubscriptionStatus(USER_ID);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("ACTIVE 구독이 없으면 SUBSCRIPTION_NOT_FOUND 예외 발생")
        void throwsWhenNoActiveSubscription() {
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionQueryService.getSubscriptionStatus(USER_ID))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("checkNoActiveSubscription()")
    class CheckNoActiveSubscription {

        @Test
        @DisplayName("ACTIVE 구독이 없으면 예외 없이 통과한다")
        void passesWhenNoActiveSubscription() {
            given(subscriptionRepository.findActiveByUserId(USER_ID)).willReturn(Optional.empty());

            subscriptionQueryService.checkNoActiveSubscription(USER_ID);
        }

        @Test
        @DisplayName("ACTIVE 구독이 존재하면 ACTIVE_SUBSCRIPTION_EXISTS 예외 발생")
        void throwsWhenActiveSubscriptionExists() {
            given(subscriptionRepository.findActiveByUserId(USER_ID))
                    .willReturn(Optional.of(activeSubscription()));

            assertThatThrownBy(() -> subscriptionQueryService.checkNoActiveSubscription(USER_ID))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS);
        }
    }
}
