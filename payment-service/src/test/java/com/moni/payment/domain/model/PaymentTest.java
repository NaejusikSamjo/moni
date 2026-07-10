package com.moni.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.PaymentCompletedEvent;
import com.moni.payment.domain.event.PaymentFailedEvent;
import com.moni.payment.domain.event.PaymentInitiatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment 도메인 테스트")
class PaymentTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Money AMOUNT = Money.of(new BigDecimal("9900"));
    private static final String ACTOR = "test-user";

    private Payment pendingPayment() {
        return Payment.create(USER_ID, MerchantId.generate(), AMOUNT,
                PaymentType.SUBSCRIPTION_INITIAL, Instant.now().plusSeconds(600), ACTOR);
    }

    @Nested
    @DisplayName("Payment.create()")
    class Create {

        @Test
        void 생성_시_상태는_PENDING이다() {
            Payment payment = pendingPayment();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        void 생성_시_userId와_amount가_설정된다() {
            Payment payment = pendingPayment();

            assertThat(payment.getUserId()).isEqualTo(USER_ID);
            assertThat(payment.getAmount()).isEqualTo(AMOUNT);
        }

        @Test
        void 생성_시_pgPaymentKey는_null이다() {
            Payment payment = pendingPayment();

            assertThat(payment.getPgPaymentKey()).isNull();
        }

        @Test
        void 생성_시_PaymentInitiatedEvent가_발행된다() {
            Payment payment = pendingPayment();

            List<Object> events = payment.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PaymentInitiatedEvent.class);
        }

        @Test
        void 생성_시_id가_null이_아니다() {
            Payment payment = pendingPayment();

            assertThat(payment.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("payment.complete()")
    class Complete {

        @Test
        void PENDING에서_COMPLETED로_전이한다() {
            Payment payment = pendingPayment();

            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        void complete_후_pgPaymentKey가_설정된다() {
            Payment payment = pendingPayment();

            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            assertThat(payment.getPgPaymentKey()).isEqualTo("pg-key-001");
        }

        @Test
        void complete_후_PaymentHistory에_전이_기록이_남는다() {
            Payment payment = pendingPayment();

            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            PaymentHistory history = payment.pullLatestHistory();
            assertThat(history.getFromStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(history.getToStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        void complete_후_PaymentCompletedEvent가_발행된다() {
            Payment payment = pendingPayment();
            payment.pullDomainEvents(); // create 이벤트 소비

            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            List<Object> events = payment.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PaymentCompletedEvent.class);
        }

        @Test
        void complete_후_PaymentCompletedEvent에_paymentType이_담긴다() {
            Payment payment = pendingPayment(); // SUBSCRIPTION_INITIAL
            payment.pullDomainEvents();

            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            PaymentCompletedEvent event = (PaymentCompletedEvent) payment.pullDomainEvents().get(0);
            assertThat(event.paymentType()).isEqualTo(PaymentType.SUBSCRIPTION_INITIAL);
        }

        @Test
        void COMPLETED_상태에서_다시_complete_호출_시_PAY_001_예외가_발생한다() {
            Payment payment = pendingPayment();
            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            assertThatThrownBy(() ->
                    payment.complete("pg-key-002", "billing-key-002", "{}", Instant.now(), ACTOR))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
        }

        @Test
        void FAILED_상태에서_complete_호출_시_PAY_001_예외가_발생한다() {
            Payment payment = pendingPayment();
            payment.fail("{}", Instant.now(), ACTOR);

            assertThatThrownBy(() ->
                    payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("payment.fail()")
    class Fail {

        @Test
        void PENDING에서_FAILED로_전이한다() {
            Payment payment = pendingPayment();

            payment.fail("PG_ERROR", Instant.now(), ACTOR);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        void fail_후_PaymentHistory에_전이_기록이_남는다() {
            Payment payment = pendingPayment();

            payment.fail("PG_ERROR", Instant.now(), ACTOR);

            PaymentHistory history = payment.pullLatestHistory();
            assertThat(history.getFromStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(history.getToStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        void fail_후_PaymentFailedEvent가_발행된다() {
            Payment payment = pendingPayment();
            payment.pullDomainEvents(); // create 이벤트 소비

            payment.fail("PG_ERROR", Instant.now(), ACTOR);

            List<Object> events = payment.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PaymentFailedEvent.class);
        }

        @Test
        void COMPLETED_상태에서_fail_호출_시_PAY_001_예외가_발생한다() {
            Payment payment = pendingPayment();
            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            assertThatThrownBy(() -> payment.fail("PG_ERROR", Instant.now(), ACTOR))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
        }

        @Test
        void FAILED_상태에서_다시_fail_호출_시_PAY_001_예외가_발생한다() {
            Payment payment = pendingPayment();
            payment.fail("PG_ERROR", Instant.now(), ACTOR);

            assertThatThrownBy(() -> payment.fail("PG_ERROR_2", Instant.now(), ACTOR))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("payment.pullLatestHistory()")
    class PullLatestHistory {

        @Test
        void 이력이_없을_때_호출_시_예외가_발생한다() {
            Payment payment = pendingPayment();

            assertThatThrownBy(() -> payment.pullLatestHistory())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void 이력이_있으면_마지막_이력을_반환한다() {
            Payment payment = pendingPayment();
            payment.complete("pg-key-001", "billing-key-001", "{}", Instant.now(), ACTOR);

            PaymentHistory latest = payment.pullLatestHistory();

            assertThat(latest.getToStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("payment.pullDomainEvents()")
    class PullDomainEvents {

        @Test
        void pullDomainEvents_호출_후_이벤트_목록이_비워진다() {
            Payment payment = pendingPayment();

            payment.pullDomainEvents();

            assertThat(payment.pullDomainEvents()).isEmpty();
        }

        @Test
        void pullDomainEvents는_현재_이벤트_목록의_복사본을_반환한다() {
            Payment payment = pendingPayment();

            List<Object> events = payment.pullDomainEvents();

            assertThat(events).hasSize(1);
        }
    }
}
