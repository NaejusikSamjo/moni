package com.moni.payment.application.service;

import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.application.service.command.SubscriptionCommandService;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribeScheduleUseCase 테스트")
class SubscribeScheduleUseCaseTest {

    @Mock private SubscriptionJpaRepository subscriptionJpaRepository;
    @Mock private PaymentCommandService paymentCommandService;
    @Mock private SubscriptionCommandService subscriptionCommandService;
    @Mock private TossPaymentsAdapter tossPaymentsAdapter;

    @InjectMocks
    private SubscribeScheduleUseCase subscribeScheduleUseCase;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final TossPaymentsAdapter.PgPaymentResult PG_SUCCESS =
            new TossPaymentsAdapter.PgPaymentResult("pg-key-001", "billing-key-001", "{}", true);
    private static final TossPaymentsAdapter.PgPaymentResult PG_FAILED =
            new TossPaymentsAdapter.PgPaymentResult("", "", "{\"code\":\"REJECT\"}", false);

    private Subscription activeSubscriptionWithAmount() {
        Subscription s = Subscription.create(UUID.randomUUID());
        s.activate(BillingKey.of("billing-key-001"), Money.of(new BigDecimal("9900")));
        return s;
    }

    private Subscription activeSubscriptionWithoutAmount() {
        // reconstitute를 사용해 amount = null인 ACTIVE 구독 생성
        return Subscription.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(),
                BillingKey.of("billing-key-001"),
                SubscriptionStatus.ACTIVE,
                LocalDate.now(),
                Instant.now(), Instant.now(), 0L,
                Collections.emptyList());
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        void 정기결제_대상이_없으면_아무_처리도_하지_않는다() {
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(Collections.emptyList());

            subscribeScheduleUseCase.execute();

            then(paymentCommandService).should(never()).recordPendingPayment(any());
        }

        @Test
        void amount가_null인_구독은_건너뛴다() {
            Subscription subscription = activeSubscriptionWithoutAmount();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(subscription));

            subscribeScheduleUseCase.execute();

            then(paymentCommandService).should(never()).recordPendingPayment(any());
        }

        @Test
        void TossPayments_성공_시_approvePayment와_extendBillingDate와_resetRetryCount를_호출한다() {
            Subscription subscription = activeSubscriptionWithAmount();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(subscription));
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_SUCCESS);

            subscribeScheduleUseCase.execute();

            then(paymentCommandService).should().approvePayment(any());
            then(subscriptionCommandService).should().extendBillingDate(eq(subscription.getId()), any());
            then(subscriptionCommandService).should().resetRetryCount(subscription.getId());
        }

        @Test
        void TossPayments_예외_시_failPayment와_handlePaymentFailure를_호출한다() {
            Subscription subscription = activeSubscriptionWithAmount();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(subscription));
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any()))
                    .willThrow(new RuntimeException("PG 네트워크 오류"));

            subscribeScheduleUseCase.execute();

            then(paymentCommandService).should().failPayment(any());
            then(subscriptionCommandService).should().handlePaymentFailure(subscription.getId());
            then(paymentCommandService).should(never()).approvePayment(any());
        }

        @Test
        void TossPayments_거절_시_failPayment와_handlePaymentFailure를_호출한다() {
            Subscription subscription = activeSubscriptionWithAmount();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(subscription));
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_FAILED);

            subscribeScheduleUseCase.execute();

            then(paymentCommandService).should().failPayment(any());
            then(subscriptionCommandService).should().handlePaymentFailure(subscription.getId());
            then(paymentCommandService).should(never()).approvePayment(any());
        }

        @Test
        void 결제_실패_3건_처리_시_handlePaymentFailure가_3회_호출된다() {
            Subscription s1 = activeSubscriptionWithAmount();
            Subscription s2 = activeSubscriptionWithAmount();
            Subscription s3 = activeSubscriptionWithAmount();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(s1, s2, s3));
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_FAILED);

            subscribeScheduleUseCase.execute();

            then(subscriptionCommandService).should().handlePaymentFailure(s1.getId());
            then(subscriptionCommandService).should().handlePaymentFailure(s2.getId());
            then(subscriptionCommandService).should().handlePaymentFailure(s3.getId());
        }

        @Test
        void 정기결제_성공_시_activateSubscription은_호출되지_않는다() {
            Subscription subscription = activeSubscriptionWithAmount();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(subscription));
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_SUCCESS);

            subscribeScheduleUseCase.execute();

            then(subscriptionCommandService).should(never()).activateSubscription(any());
        }

        @Test
        void 한_건_처리_실패가_다음_구독_처리를_중단시키지_않는다() {
            Subscription first = activeSubscriptionWithAmount();
            Subscription second = activeSubscriptionWithAmount();
            given(subscriptionJpaRepository.findActiveSubscriptionsDueBefore(
                    eq(SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                    .willReturn(List.of(first, second));
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            // 첫 번째 구독에서 예외 발생
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any()))
                    .willThrow(new RuntimeException("첫 번째 실패"))
                    .willReturn(PG_SUCCESS);

            subscribeScheduleUseCase.execute();

            // 두 번째 구독은 approvePayment까지 정상 처리
            then(paymentCommandService).should().approvePayment(any());
        }
    }
}
