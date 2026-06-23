package com.moni.payment.domain.model;

import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.PaymentCompletedEvent;
import com.moni.payment.domain.event.PaymentFailedEvent;
import com.moni.payment.domain.event.PaymentInitiatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment Aggregate")
class PaymentTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final MerchantId MERCHANT_ID = MerchantId.of("moni-sub-001");
    private static final Money AMOUNT = Money.of(9900L);
    private static final Instant EXPIRES_AT = Instant.now().plusSeconds(600);
    private static final String ACTOR = "user-system";

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.initiate(USER_ID, MERCHANT_ID, AMOUNT, PaymentType.SUBSCRIPTION_INITIAL,
                EXPIRES_AT, ACTOR);
    }

    @Nested
    @DisplayName("initiate()")
    class Initiate {

        @Test
        @DisplayName("initiate() 후 상태는 PENDING이다")
        void statusIsPending() {
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("initiate() 후 pgPaymentKey는 null이다")
        void pgPaymentKeyIsNull() {
            assertThat(payment.getPgPaymentKey()).isNull();
        }

        @Test
        @DisplayName("initiate() 후 PaymentInitiatedEvent가 발행된다")
        void emitsInitiatedEvent() {
            List<Object> events = payment.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PaymentInitiatedEvent.class);

            PaymentInitiatedEvent event = (PaymentInitiatedEvent) events.get(0);
            assertThat(event.paymentId()).isEqualTo(payment.getId());
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.amount()).isEqualTo(AMOUNT);
        }

        @Test
        @DisplayName("pullDomainEvents() 후 이벤트 목록은 비워진다")
        void pullClearsEvents() {
            payment.pullDomainEvents();
            assertThat(payment.pullDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("complete()")
    class Complete {

        private static final String PG_KEY = "toss_pg_key_abc";

        @Test
        @DisplayName("PENDING → COMPLETED 전이 후 상태가 변경된다")
        void statusBecomesCompleted() {
            payment.pullDomainEvents(); // clear initiate event
            payment.complete(PG_KEY, "{}", Instant.now(), ACTOR);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getPgPaymentKey()).isEqualTo(PG_KEY);
        }

        @Test
        @DisplayName("complete() 후 PaymentCompletedEvent가 발행된다")
        void emitsCompletedEvent() {
            payment.pullDomainEvents();
            payment.complete(PG_KEY, "{}", Instant.now(), ACTOR);

            List<Object> events = payment.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PaymentCompletedEvent.class);

            PaymentCompletedEvent event = (PaymentCompletedEvent) events.get(0);
            assertThat(event.pgPaymentKey()).isEqualTo(PG_KEY);
        }

        @Test
        @DisplayName("complete() 후 histories에 이력이 추가된다")
        void historyIsAdded() {
            payment.pullDomainEvents();
            payment.complete(PG_KEY, "{\"status\":\"DONE\"}", Instant.now(), ACTOR);

            assertThat(payment.getHistories()).hasSize(1);
            PaymentHistory history = payment.getHistories().get(0);
            assertThat(history.getFromStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(history.getToStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 complete() 재호출 시 예외 발생")
        void cannotCompleteAgain() {
            payment.complete(PG_KEY, "{}", Instant.now(), ACTOR);
            assertThatThrownBy(() -> payment.complete("another_key", "{}", Instant.now(), ACTOR))
                    .isInstanceOf(PaymentException.class);
        }
    }

    @Nested
    @DisplayName("fail()")
    class Fail {

        @Test
        @DisplayName("PENDING → FAILED 전이 후 상태가 변경된다")
        void statusBecomesFailed() {
            payment.pullDomainEvents();
            payment.fail("{\"error\":\"INSUFFICIENT_BALANCE\"}", Instant.now(), ACTOR);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("fail() 후 PaymentFailedEvent가 발행된다")
        void emitsFailedEvent() {
            payment.pullDomainEvents();
            payment.fail("{}", Instant.now(), ACTOR);

            List<Object> events = payment.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PaymentFailedEvent.class);
        }

        @Test
        @DisplayName("fail() 후 histories에 이력이 추가된다")
        void historyIsAdded() {
            payment.pullDomainEvents();
            payment.fail("{}", Instant.now(), ACTOR);

            assertThat(payment.getHistories()).hasSize(1);
            PaymentHistory history = payment.getHistories().get(0);
            assertThat(history.getFromStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(history.getToStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("FAILED 상태에서 fail() 재호출 시 예외 발생 (종단 상태)")
        void cannotFailAgain() {
            payment.fail("{}", Instant.now(), ACTOR);
            assertThatThrownBy(() -> payment.fail("{}", Instant.now(), ACTOR))
                    .isInstanceOf(PaymentException.class);
        }
    }

    @Nested
    @DisplayName("getHistories()")
    class Histories {

        @Test
        @DisplayName("반환된 histories 목록은 불변이다")
        void historiesIsUnmodifiable() {
            List<PaymentHistory> histories = payment.getHistories();
            assertThatThrownBy(() -> histories.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("저장소에서 복원된 Payment는 입력 필드를 그대로 갖는다")
        void reconstitutePreservesFields() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            Payment restored = Payment.reconstitute(
                    id, MERCHANT_ID, USER_ID, PaymentType.SUBSCRIPTION_RECURRING,
                    AMOUNT, "pg-key", PaymentStatus.COMPLETED,
                    EXPIRES_AT, now, ACTOR, now, ACTOR, List.of());

            assertThat(restored.getId()).isEqualTo(id);
            assertThat(restored.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(restored.getPgPaymentKey()).isEqualTo("pg-key");
            assertThat(restored.pullDomainEvents()).isEmpty();
        }
    }
}
