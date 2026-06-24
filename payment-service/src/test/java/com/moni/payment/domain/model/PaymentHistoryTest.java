package com.moni.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentHistory 도메인 단위 테스트")
class PaymentHistoryTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final String PG_RESPONSE = "{\"status\":\"DONE\"}";
    private static final String ACTOR = "user-001";

    @Nested
    @DisplayName("of()")
    class Of {

        @Test
        @DisplayName("입력값을 올바르게 저장한다")
        void savesAllFields() {
            Instant requestedAt = Instant.now();

            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    PG_RESPONSE, requestedAt, ACTOR);

            assertThat(history.getId()).isNotNull();
            assertThat(history.getPaymentId()).isEqualTo(PAYMENT_ID);
            assertThat(history.getFromStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(history.getToStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(history.getPgResponse()).isEqualTo(PG_RESPONSE);
            assertThat(history.getRequestedAt()).isEqualTo(requestedAt);
            assertThat(history.getRequestedBy()).isEqualTo(ACTOR);
        }

        @Test
        @DisplayName("생성 시 respondedAt은 null이다")
        void respondedAtIsNullOnCreation() {
            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    PG_RESPONSE, Instant.now(), ACTOR);

            assertThat(history.getRespondedAt()).isNull();
        }

        @Test
        @DisplayName("각 호출마다 고유한 id가 생성된다")
        void generatesUniqueId() {
            PaymentHistory history1 = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    "{}", Instant.now(), ACTOR);
            PaymentHistory history2 = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.FAILED,
                    "{}", Instant.now(), ACTOR);

            assertThat(history1.getId()).isNotEqualTo(history2.getId());
        }
    }

    @Nested
    @DisplayName("recordResponse()")
    class RecordResponse {

        @Test
        @DisplayName("respondedAt이 설정된다")
        void setsRespondedAt() {
            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    PG_RESPONSE, Instant.now(), ACTOR);
            Instant respondedAt = Instant.now();

            history.recordResponse(respondedAt);

            assertThat(history.getRespondedAt()).isEqualTo(respondedAt);
        }

        @Test
        @DisplayName("여러 번 호출하면 마지막 값이 유지된다")
        void overwritesPreviousRespondedAt() {
            PaymentHistory history = PaymentHistory.of(
                    PAYMENT_ID, PaymentStatus.PENDING, PaymentStatus.COMPLETED,
                    PG_RESPONSE, Instant.now(), ACTOR);

            Instant first = Instant.now().minusSeconds(10);
            Instant second = Instant.now();
            history.recordResponse(first);
            history.recordResponse(second);

            assertThat(history.getRespondedAt()).isEqualTo(second);
        }
    }

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("모든 필드를 복원한다")
        void restoresAllFields() {
            UUID id = UUID.randomUUID();
            Instant requestedAt = Instant.now().minusSeconds(10);
            Instant respondedAt = Instant.now();

            PaymentHistory history = PaymentHistory.reconstitute(
                    id, PAYMENT_ID,
                    PaymentStatus.PENDING, PaymentStatus.FAILED,
                    "{\"error\":\"CARD_LIMIT\"}", requestedAt, ACTOR, respondedAt);

            assertThat(history.getId()).isEqualTo(id);
            assertThat(history.getPaymentId()).isEqualTo(PAYMENT_ID);
            assertThat(history.getFromStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(history.getToStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(history.getPgResponse()).isEqualTo("{\"error\":\"CARD_LIMIT\"}");
            assertThat(history.getRequestedAt()).isEqualTo(requestedAt);
            assertThat(history.getRequestedBy()).isEqualTo(ACTOR);
            assertThat(history.getRespondedAt()).isEqualTo(respondedAt);
        }
    }
}
