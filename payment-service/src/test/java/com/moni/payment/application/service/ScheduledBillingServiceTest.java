package com.moni.payment.application.service;

import com.moni.payment.application.repository.PaymentRepository;
import com.moni.payment.application.repository.PgPaymentClient;
import com.moni.payment.application.repository.SubscriptionEventPublisher;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledBillingService — processScheduledBilling()")
class ScheduledBillingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PgPaymentClient pgPaymentClient;
    @Mock
    private SubscriptionEventPublisher subscriptionEventPublisher;

    private ScheduledBillingService scheduledBillingService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final String BILLING_KEY = "bk-test-001";
    private static final String PG_PAYMENT_KEY = "toss_pg_scheduled_001";

    @BeforeEach
    void setUp() {
        scheduledBillingService = new ScheduledBillingService(
                subscriptionRepository, paymentRepository, pgPaymentClient, subscriptionEventPublisher);
    }

    private Subscription activeSubscription() {
        Instant now = Instant.now();
        return Subscription.reconstitute(
                SUBSCRIPTION_ID, USER_ID,
                BillingKey.of(BILLING_KEY),
                SubscriptionStatus.ACTIVE,
                LocalDate.now().minusDays(1),
                now, now, 1L, List.of());
    }

    private PgPaymentClient.PgPaymentResult successResult() {
        return new PgPaymentClient.PgPaymentResult(PG_PAYMENT_KEY, BILLING_KEY, "{\"status\":\"DONE\"}", true);
    }

    @Nested
    @DisplayName("대상 구독 없음")
    class NoSubscriptionsDue {

        @Test
        @DisplayName("정기결제 대상이 없으면 PG 호출 없이 종료된다")
        void noOpWhenNoDueSubscriptions() {
            given(subscriptionRepository.findActiveSubscriptionsDueBefore(any()))
                    .willReturn(Collections.emptyList());

            scheduledBillingService.processScheduledBilling();

            then(pgPaymentClient).should(never()).requestBillingPayment(anyString(), any(), any());
            then(paymentRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("정기결제 성공")
    class BillingSuccess {

        @BeforeEach
        void setUp() {
            given(subscriptionRepository.findActiveSubscriptionsDueBefore(any()))
                    .willReturn(List.of(activeSubscription()));
            given(pgPaymentClient.requestBillingPayment(anyString(), any(), any())).willReturn(successResult());
            given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("결제 성공 시 Payment가 COMPLETED 상태로 저장된다")
        void paymentSavedAsCompleted() {
            scheduledBillingService.processScheduledBilling();

            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            then(paymentRepository).should().save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(paymentCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("결제 성공 시 PaymentHistory가 저장된다")
        void paymentHistorySaved() {
            scheduledBillingService.processScheduledBilling();
            then(paymentRepository).should(times(1)).saveHistory(any());
        }

        @Test
        @DisplayName("결제 성공 시 구독의 nextBillingDate가 1개월 연장된다")
        void nextBillingDateExtended() {
            scheduledBillingService.processScheduledBilling();

            ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
            then(subscriptionRepository).should().save(subscriptionCaptor.capture());
            assertThat(subscriptionCaptor.getValue().getNextBillingDate())
                    .isEqualTo(LocalDate.now().plusMonths(1));
        }

        @Test
        @DisplayName("결제 성공 시 BillingFailedEvent는 발행되지 않는다")
        void noBillingFailedEventOnSuccess() {
            scheduledBillingService.processScheduledBilling();
            then(subscriptionEventPublisher).should(never()).publishBillingFailed(any());
        }
    }

    @Nested
    @DisplayName("정기결제 실패 - PG success=false")
    class BillingFailure {

        @BeforeEach
        void setUp() {
            given(subscriptionRepository.findActiveSubscriptionsDueBefore(any()))
                    .willReturn(List.of(activeSubscription()));
            given(pgPaymentClient.requestBillingPayment(anyString(), any(), any()))
                    .willReturn(new PgPaymentClient.PgPaymentResult(null, null, "{\"error\":\"CARD_LIMIT\"}", false));
            given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("PG 실패 시 구독 상태가 SUSPENDED로 변경된다")
        void subscriptionSuspendedOnFailure() {
            scheduledBillingService.processScheduledBilling();

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            then(subscriptionRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        @DisplayName("PG 실패 시 SubscriptionHistory가 저장된다")
        void historyIsSavedOnFailure() {
            scheduledBillingService.processScheduledBilling();
            then(subscriptionRepository).should().saveHistory(any(SubscriptionHistory.class));
        }

        @Test
        @DisplayName("PG 실패 시 BillingFailedEvent가 발행된다")
        void billingFailedEventPublished() {
            scheduledBillingService.processScheduledBilling();

            ArgumentCaptor<BillingFailedEvent> captor = ArgumentCaptor.forClass(BillingFailedEvent.class);
            then(subscriptionEventPublisher).should().publishBillingFailed(captor.capture());
            assertThat(captor.getValue().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().amount()).isEqualTo(ScheduledBillingService.SUBSCRIPTION_AMOUNT);
        }

        @Test
        @DisplayName("PG 실패 시 Payment는 저장되지 않는다")
        void paymentNotSavedOnFailure() {
            scheduledBillingService.processScheduledBilling();
            then(paymentRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("정기결제 실패 - PG 예외")
    class BillingException {

        @BeforeEach
        void setUp() {
            given(subscriptionRepository.findActiveSubscriptionsDueBefore(any()))
                    .willReturn(List.of(activeSubscription()));
            given(pgPaymentClient.requestBillingPayment(anyString(), any(), any()))
                    .willThrow(new PaymentException(PaymentErrorCode.PG_CONNECTION_TIMEOUT));
            given(subscriptionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("PG 타임아웃 예외 시 구독이 SUSPENDED 상태로 변경된다")
        void subscriptionSuspendedOnException() {
            scheduledBillingService.processScheduledBilling();

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            then(subscriptionRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        @DisplayName("PG 예외 시에도 BillingFailedEvent가 발행된다")
        void billingFailedEventPublishedOnException() {
            scheduledBillingService.processScheduledBilling();
            then(subscriptionEventPublisher).should().publishBillingFailed(any(BillingFailedEvent.class));
        }
    }
}
