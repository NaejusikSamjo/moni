package com.moni.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentHistory 도메인 테스트")
class PaymentHistoryTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final String ACTOR = "test-user";

    @Nested
    @DisplayName("PaymentHistory.of()")
    class Of {

        @Test
        void 생성_시_paymentId와_상태_전이_정보가_설정된다() {
            Instant requestedAt = Instant.now();

            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    "{}", requestedAt, ACTOR);

            assertThat(history.getPaymentId()).isEqualTo(PAYMENT_ID);
            assertThat(history.getFromStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(history.getToStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        void 생성_시_id가_자동으로_부여된다() {
            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.FAILED,
                    "PG_ERROR", Instant.now(), ACTOR);

            assertThat(history.getId()).isNotNull();
        }

        @Test
        void 생성_시_respondedAt은_null이다() {
            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    "{}", Instant.now(), ACTOR);

            assertThat(history.getRespondedAt()).isNull();
        }

        @Test
        void 두_번_생성하면_서로_다른_id를_가진다() {
            PaymentHistory history1 = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    "{}", Instant.now(), ACTOR);
            PaymentHistory history2 = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.FAILED,
                    "ERROR", Instant.now(), ACTOR);

            assertThat(history1.getId()).isNotEqualTo(history2.getId());
        }
    }

    @Nested
    @DisplayName("paymentHistory.recordResponse()")
    class RecordResponse {

        @Test
        void recordResponse_호출_후_respondedAt이_설정된다() {
            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    "{}", Instant.now(), ACTOR);
            Instant respondedAt = Instant.now();

            history.recordResponse(respondedAt);

            assertThat(history.getRespondedAt()).isEqualTo(respondedAt);
        }
    }
}
