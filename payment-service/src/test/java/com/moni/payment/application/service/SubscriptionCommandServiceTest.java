package com.moni.payment.application.service;

import com.moni.payment.application.command.ActivateSubscriptionCommand;
import com.moni.payment.application.command.CancelSubscriptionCommand;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionCommandService")
class SubscriptionCommandServiceTest {

    @Mock
    private SubscriptionJpaRepository subscriptionJpaRepository;
    @Mock
    private SubscriptionHistoryRepository subscriptionHistoryRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SubscriptionCommandService subscriptionCommandService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String BILLING_KEY_VALUE = "bk-toss-001";

    @BeforeEach
    void setUp() {
        subscriptionCommandService = new SubscriptionCommandService(
                subscriptionJpaRepository, subscriptionHistoryRepository, applicationEventPublisher);
    }

    private Subscription activeSubscription() {
        Instant now = Instant.now();
        return Subscription.reconstitute(
                UUID.randomUUID(), USER_ID, BillingKey.of(BILLING_KEY_VALUE),
                SubscriptionStatus.ACTIVE, LocalDate.now().plusMonths(1),
                now, now, 1L, List.of());
    }

    @Nested
    @DisplayName("activateSubscription()")
    class ActivateSubscription {

        @BeforeEach
        void setUp() {
            given(subscriptionJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("저장된 Subscription 상태가 ACTIVE이다")
        void subscriptionIsActive() {
            Subscription result = subscriptionCommandService.activateSubscription(
                    new ActivateSubscriptionCommand(USER_ID, BILLING_KEY_VALUE));

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getBillingKey().getValue()).isEqualTo(BILLING_KEY_VALUE);
        }

        @Test
        @DisplayName("nextBillingDate가 오늘 + 1개월로 설정된다")
        void nextBillingDateIsNextMonth() {
            Subscription result = subscriptionCommandService.activateSubscription(
                    new ActivateSubscriptionCommand(USER_ID, BILLING_KEY_VALUE));

            assertThat(result.getNextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
        }

        @Test
        @DisplayName("SubscriptionHistory가 저장된다")
        void historyIsSaved() {
            subscriptionCommandService.activateSubscription(
                    new ActivateSubscriptionCommand(USER_ID, BILLING_KEY_VALUE));

            then(subscriptionHistoryRepository).should().save(any(SubscriptionHistory.class));
        }

        @Test
        @DisplayName("SubscriptionActivatedEvent가 ApplicationEventPublisher를 통해 발행된다")
        void publishesActivatedEvent() {
            subscriptionCommandService.activateSubscription(
                    new ActivateSubscriptionCommand(USER_ID, BILLING_KEY_VALUE));

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            then(applicationEventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(SubscriptionActivatedEvent.class);
            SubscriptionActivatedEvent event = (SubscriptionActivatedEvent) captor.getValue();
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.billingKey().getValue()).isEqualTo(BILLING_KEY_VALUE);
        }
    }

    @Nested
    @DisplayName("cancelSubscription()")
    class CancelSubscription {

        private Subscription subscription;

        @BeforeEach
        void setUp() {
            subscription = activeSubscription();
            given(subscriptionJpaRepository.findById(subscription.getId()))
                    .willReturn(Optional.of(subscription));
        }

        @Test
        @DisplayName("구독 상태가 CANCELLING으로 변경된다")
        void statusBecomesCancelling() {
            given(subscriptionJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            subscriptionCommandService.cancelSubscription(
                    new CancelSubscriptionCommand(subscription.getId(), USER_ID, "사용자 요청"));

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            then(subscriptionJpaRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLING);
        }

        @Test
        @DisplayName("SubscriptionCancelledEvent가 ApplicationEventPublisher를 통해 발행된다")
        void publishesCancelledEvent() {
            given(subscriptionJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            subscriptionCommandService.cancelSubscription(
                    new CancelSubscriptionCommand(subscription.getId(), USER_ID, "환불 요청"));

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            then(applicationEventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(SubscriptionCancelledEvent.class);
            SubscriptionCancelledEvent event = (SubscriptionCancelledEvent) captor.getValue();
            assertThat(event.userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("본인 구독이 아니면 SUBSCRIPTION_NOT_FOUND 예외 발생")
        void throwsWhenSubscriptionBelongsToOtherUser() {
            UUID otherId = UUID.randomUUID();
            assertThatThrownBy(() -> subscriptionCommandService.cancelSubscription(
                    new CancelSubscriptionCommand(subscription.getId(), otherId, "이유")))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
    }
}
