package com.moni.payment.application.service;

import com.moni.payment.application.dto.command.SubscribeResult;
import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.application.service.command.SubscriptionCommandService;
import com.moni.payment.application.service.command.SubscriptionQueryService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateSubscriptionUseCase 테스트")
class ReactivateSubscriptionUseCaseTest {

    @Mock private SubscriptionQueryService subscriptionQueryService;
    @Mock private PaymentCommandService paymentCommandService;
    @Mock private SubscriptionCommandService subscriptionCommandService;
    @Mock private TossPaymentsAdapter tossPaymentsAdapter;

    @InjectMocks
    private ReactivateSubscriptionUseCase reactivateSubscriptionUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final TossPaymentsAdapter.PgPaymentResult PG_SUCCESS =
            new TossPaymentsAdapter.PgPaymentResult("pg-key-001", "billing-key-001", "{}", true);
    private static final TossPaymentsAdapter.PgPaymentResult PG_FAILED =
            new TossPaymentsAdapter.PgPaymentResult("", "", "{\"code\":\"REJECT\"}", false);

    private Subscription suspendedSubscription() {
        Subscription s = Subscription.create(USER_ID);
        s.activate(BillingKey.of("billing-key-001"), Money.of(new BigDecimal("9900")));
        s.suspend("결제 3회 연속 실패");
        return s;
    }

    private Subscription suspendedSubscriptionWithoutAmount() {
        return Subscription.reconstitute(
                UUID.randomUUID(), USER_ID,
                BillingKey.of("billing-key-001"),
                SubscriptionStatus.SUSPENDED,
                LocalDate.now(),
                Instant.now(), Instant.now(), 0L,
                Collections.emptyList());
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        void SUSPENDED_구독이_없으면_SUB_002_예외가_발생한다() {
            willThrow(new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND))
                    .given(subscriptionQueryService).findSuspendedSubscription(USER_ID);

            assertThatThrownBy(() -> reactivateSubscriptionUseCase.execute(USER_ID))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
        }

        @Test
        void amount가_null이면_SUB_002_예외가_발생한다() {
            given(subscriptionQueryService.findSuspendedSubscription(USER_ID))
                    .willReturn(suspendedSubscriptionWithoutAmount());

            assertThatThrownBy(() -> reactivateSubscriptionUseCase.execute(USER_ID))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
        }

        @Nested
        @DisplayName("SUSPENDED 구독 재활성화 결제")
        class Payment {

            @BeforeEach
            void setUp() {
                given(subscriptionQueryService.findSuspendedSubscription(USER_ID))
                        .willReturn(suspendedSubscription());
                given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            }

            @Test
            void TossPayments_성공_시_COMPLETED_결과를_반환한다() {
                given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_SUCCESS);

                SubscribeResult result = reactivateSubscriptionUseCase.execute(USER_ID);

                assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);
                assertThat(result.status()).isEqualTo("COMPLETED");
                assertThat(result.nextBillingDate()).isNotNull();
            }

            @Test
            void TossPayments_성공_시_approvePayment와_reactivateFromSuspended를_호출한다() {
                Subscription suspended = suspendedSubscription();
                given(subscriptionQueryService.findSuspendedSubscription(USER_ID))
                        .willReturn(suspended);
                given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_SUCCESS);

                reactivateSubscriptionUseCase.execute(USER_ID);

                then(paymentCommandService).should().approvePayment(any());
                then(subscriptionCommandService).should().reactivateFromSuspended(suspended.getId());
            }

            @Test
            void TossPayments_예외_시_failPayment를_호출하고_PG_PAYMENT_FAILED_예외가_발생한다() {
                given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any()))
                        .willThrow(new RuntimeException("PG 네트워크 오류"));

                assertThatThrownBy(() -> reactivateSubscriptionUseCase.execute(USER_ID))
                        .isInstanceOf(PaymentException.class)
                        .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                                .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED));

                then(paymentCommandService).should().failPayment(any());
            }

            @Test
            void TossPayments_거절_시_failPayment를_호출하고_PG_PAYMENT_FAILED_예외가_발생한다() {
                given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_FAILED);

                assertThatThrownBy(() -> reactivateSubscriptionUseCase.execute(USER_ID))
                        .isInstanceOf(PaymentException.class)
                        .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                                .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED));

                then(paymentCommandService).should().failPayment(any());
            }
        }
    }
}
