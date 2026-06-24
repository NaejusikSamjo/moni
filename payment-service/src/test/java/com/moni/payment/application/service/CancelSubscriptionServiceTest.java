package com.moni.payment.application.service;

import com.moni.payment.application.repository.SubscriptionEventPublisher;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.CancelSubscriptionUseCase;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService — cancelSubscription()")
class CancelSubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionEventPublisher subscriptionEventPublisher;

    private SubscriptionService subscriptionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final String REASON = "사용자 요청";

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(subscriptionRepository, subscriptionEventPublisher);
    }

    private Subscription activeSubscription() {
        Instant now = Instant.now();
        return Subscription.reconstitute(
                SUBSCRIPTION_ID, USER_ID,
                BillingKey.of("bk-test-001"),
                SubscriptionStatus.ACTIVE,
                LocalDate.now().plusMonths(1),
                now, now, 1L, List.of());
    }

    private CancelSubscriptionUseCase.CancelSubscriptionCommand command() {
        return new CancelSubscriptionUseCase.CancelSubscriptionCommand(SUBSCRIPTION_ID, USER_ID, REASON);
    }

    @Nested
    @DisplayName("취소 성공")
    class Success {

        @BeforeEach
        void setUp() {
            given(subscriptionRepository.findById(SUBSCRIPTION_ID))
                    .willReturn(Optional.of(activeSubscription()));
            given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("구독 상태가 CANCELLING으로 변경된다")
        void statusChangesToCancelling() {
            subscriptionService.cancelSubscription(command());

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            then(subscriptionRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLING);
        }

        @Test
        @DisplayName("SubscriptionHistory가 저장된다")
        void historyIsSaved() {
            subscriptionService.cancelSubscription(command());
            then(subscriptionRepository).should().saveHistory(any());
        }

        @Test
        @DisplayName("SubscriptionCancelledEvent가 발행된다")
        void publishesCancelledEvent() {
            subscriptionService.cancelSubscription(command());

            ArgumentCaptor<SubscriptionCancelledEvent> captor =
                    ArgumentCaptor.forClass(SubscriptionCancelledEvent.class);
            then(subscriptionEventPublisher).should().publishCancelled(captor.capture());

            SubscriptionCancelledEvent event = captor.getValue();
            assertThat(event.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.reason()).isEqualTo(REASON);
        }
    }

    @Nested
    @DisplayName("취소 실패 - 구독 없음")
    class NotFound {

        @Test
        @DisplayName("구독이 존재하지 않으면 SUBSCRIPTION_NOT_FOUND 예외 발생")
        void throwsWhenSubscriptionNotFound() {
            given(subscriptionRepository.findById(SUBSCRIPTION_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(command()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND);

            then(subscriptionRepository).should(never()).save(any());
            then(subscriptionEventPublisher).should(never()).publishCancelled(any());
        }
    }

    @Nested
    @DisplayName("취소 실패 - 사용자 불일치")
    class WrongUser {

        @Test
        @DisplayName("구독의 userId가 요청 userId와 다르면 SUBSCRIPTION_NOT_FOUND 예외 발생")
        void throwsWhenUserIdMismatch() {
            UUID differentUserId = UUID.randomUUID();
            CancelSubscriptionUseCase.CancelSubscriptionCommand wrongCmd =
                    new CancelSubscriptionUseCase.CancelSubscriptionCommand(
                            SUBSCRIPTION_ID, differentUserId, REASON);

            given(subscriptionRepository.findById(SUBSCRIPTION_ID))
                    .willReturn(Optional.of(activeSubscription()));

            assertThatThrownBy(() -> subscriptionService.cancelSubscription(wrongCmd))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND);

            then(subscriptionRepository).should(never()).save(any());
        }
    }
}
