package com.moni.payment.application.service;

import com.moni.payment.application.dto.command.SubscribeCommand;
import com.moni.payment.application.dto.command.SubscribeResult;
import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.application.service.command.SubscriptionQueryService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import com.moni.payment.infrastructure.lock.RedisSubscriptionLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscribePaymentUseCase 테스트")
class SubscribePaymentUseCaseTest {

    @Mock private SubscriptionQueryService subscriptionQueryService;
    @Mock private PaymentCommandService paymentCommandService;
    @Mock private TossPaymentsAdapter tossPaymentsAdapter;
    @Mock private RedisSubscriptionLock redisSubscriptionLock;

    @InjectMocks
    private SubscribePaymentUseCase subscribePaymentUseCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final TossPaymentsAdapter.PgPaymentResult PG_SUCCESS =
            new TossPaymentsAdapter.PgPaymentResult("pg-key-001", "billing-key-001", "{}", true);
    private static final TossPaymentsAdapter.PgPaymentResult PG_FAILED =
            new TossPaymentsAdapter.PgPaymentResult("", "", "{\"code\":\"REJECT\"}", false);

    private SubscribeCommand command;

    @BeforeEach
    void setUp() {
        command = new SubscribeCommand(USER_ID, "auth-key", "customer-key", 9900L, USER_ID.toString());
    }

    private Subscription cancellingSubscription() {
        Subscription s = Subscription.create(USER_ID);
        s.activate(BillingKey.of("existing-billing-key"), Money.of(new BigDecimal("9900")));
        s.cancel("취소");
        return s;
    }

    @Nested
    @DisplayName("Redis 락")
    class Lock {

        @Test
        void 락_획득_실패_시_DUPLICATE_PAYMENT_예외가_발생한다() {
            given(redisSubscriptionLock.tryLock(eq(USER_ID), any(Duration.class))).willReturn(false);

            assertThatThrownBy(() -> subscribePaymentUseCase.execute(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.DUPLICATE_PAYMENT));
        }

        @Test
        void 정상_처리_후_락이_해제된다() {
            given(redisSubscriptionLock.tryLock(eq(USER_ID), any(Duration.class))).willReturn(true);
            given(subscriptionQueryService.findCancellingSubscription(USER_ID)).willReturn(Optional.empty());
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            given(tossPaymentsAdapter.requestPayment(any())).willReturn(PG_SUCCESS);

            subscribePaymentUseCase.execute(command);

            then(redisSubscriptionLock).should().unlock(USER_ID);
        }

        @Test
        void PG_예외_발생_시에도_락이_해제된다() {
            given(redisSubscriptionLock.tryLock(eq(USER_ID), any(Duration.class))).willReturn(true);
            given(subscriptionQueryService.findCancellingSubscription(USER_ID)).willReturn(Optional.empty());
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
            given(tossPaymentsAdapter.requestPayment(any())).willThrow(new RuntimeException("PG 오류"));

            assertThatThrownBy(() -> subscribePaymentUseCase.execute(command))
                    .isInstanceOf(PaymentException.class);

            then(redisSubscriptionLock).should().unlock(USER_ID);
        }
    }

    @Nested
    @DisplayName("신규 구독 결제")
    class NewSubscription {

        @BeforeEach
        void setUp() {
            given(redisSubscriptionLock.tryLock(eq(USER_ID), any(Duration.class))).willReturn(true);
            given(subscriptionQueryService.findCancellingSubscription(USER_ID)).willReturn(Optional.empty());
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
        }

        @Test
        void TossPayments_성공_시_COMPLETED_결과를_반환한다() {
            given(tossPaymentsAdapter.requestPayment(any())).willReturn(PG_SUCCESS);

            SubscribeResult result = subscribePaymentUseCase.execute(command);

            assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.amount()).isEqualTo(9900L);
            assertThat(result.nextBillingDate()).isNotNull();
        }

        @Test
        void TossPayments_성공_시_approvePayment를_호출한다() {
            given(tossPaymentsAdapter.requestPayment(any())).willReturn(PG_SUCCESS);

            subscribePaymentUseCase.execute(command);

            then(paymentCommandService).should().approvePayment(any());
        }

        @Test
        void TossPayments_예외_시_failPayment를_호출하고_PG_PAYMENT_FAILED_예외가_발생한다() {
            given(tossPaymentsAdapter.requestPayment(any())).willThrow(new RuntimeException("네트워크 오류"));

            assertThatThrownBy(() -> subscribePaymentUseCase.execute(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED));

            then(paymentCommandService).should().failPayment(any());
        }

        @Test
        void TossPayments_거절_시_failPayment를_호출하고_PG_PAYMENT_FAILED_예외가_발생한다() {
            given(tossPaymentsAdapter.requestPayment(any())).willReturn(PG_FAILED);

            assertThatThrownBy(() -> subscribePaymentUseCase.execute(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED));

            then(paymentCommandService).should().failPayment(any());
        }
    }

    @Nested
    @DisplayName("CANCELLING 상태에서 재구독")
    class Reactivation {

        @BeforeEach
        void setUp() {
            given(redisSubscriptionLock.tryLock(eq(USER_ID), any(Duration.class))).willReturn(true);
            given(subscriptionQueryService.findCancellingSubscription(USER_ID))
                    .willReturn(Optional.of(cancellingSubscription()));
            given(paymentCommandService.recordPendingPayment(any())).willReturn(PAYMENT_ID);
        }

        @Test
        void 기존_빌링키로_requestBillingPayment를_호출한다() {
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_SUCCESS);

            subscribePaymentUseCase.execute(command);

            then(tossPaymentsAdapter).should().requestBillingPayment(
                    eq("existing-billing-key"), any(), any());
        }

        @Test
        void TossPayments_성공_시_COMPLETED_결과를_반환한다() {
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_SUCCESS);

            SubscribeResult result = subscribePaymentUseCase.execute(command);

            assertThat(result.status()).isEqualTo("COMPLETED");
        }

        @Test
        void TossPayments_거절_시_PG_PAYMENT_FAILED_예외가_발생한다() {
            given(tossPaymentsAdapter.requestBillingPayment(any(), any(), any())).willReturn(PG_FAILED);

            assertThatThrownBy(() -> subscribePaymentUseCase.execute(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.PG_PAYMENT_FAILED));
        }
    }

    @Nested
    @DisplayName("활성 구독 중복 검사")
    class DuplicateCheck {

        @Test
        void 활성_구독이_있으면_ACTIVE_SUBSCRIPTION_EXISTS_예외가_발생한다() {
            given(redisSubscriptionLock.tryLock(eq(USER_ID), any(Duration.class))).willReturn(true);
            willThrow(new PaymentException(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS))
                    .given(subscriptionQueryService).checkNoActiveSubscription(USER_ID);

            assertThatThrownBy(() -> subscribePaymentUseCase.execute(command))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.ACTIVE_SUBSCRIPTION_EXISTS));

            then(redisSubscriptionLock).should().unlock(USER_ID);
        }
    }
}
