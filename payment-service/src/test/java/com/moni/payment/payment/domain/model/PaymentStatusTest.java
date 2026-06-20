package com.moni.payment.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentStatus 상태 머신")
class PaymentStatusTest {

    @Nested
    @DisplayName("PENDING 전이")
    class FromPending {

        @Test
        @DisplayName("PENDING → COMPLETED 허용")
        void pendingToCompleted() {
            assertThatCode(() -> PaymentStatus.PENDING.validateTransitionTo(PaymentStatus.COMPLETED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PENDING → FAILED 허용")
        void pendingToFailed() {
            assertThatCode(() -> PaymentStatus.PENDING.validateTransitionTo(PaymentStatus.FAILED))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("PENDING → PENDING/REFUNDED 불허")
        @EnumSource(value = PaymentStatus.class, names = {"PENDING", "REFUNDED"})
        void pendingToInvalidThrows(PaymentStatus target) {
            assertThatThrownBy(() -> PaymentStatus.PENDING.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("COMPLETED 전이")
    class FromCompleted {

        @Test
        @DisplayName("COMPLETED → REFUNDED 허용")
        void completedToRefunded() {
            assertThatCode(() -> PaymentStatus.COMPLETED.validateTransitionTo(PaymentStatus.REFUNDED))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("COMPLETED → PENDING/COMPLETED/FAILED 불허")
        @EnumSource(value = PaymentStatus.class, names = {"PENDING", "COMPLETED", "FAILED"})
        void completedToInvalidThrows(PaymentStatus target) {
            assertThatThrownBy(() -> PaymentStatus.COMPLETED.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("FAILED 전이 (종단 상태)")
    class FromFailed {

        @ParameterizedTest
        @DisplayName("FAILED에서 모든 상태 전이 불허")
        @EnumSource(PaymentStatus.class)
        void failedIsTerminal(PaymentStatus target) {
            assertThatThrownBy(() -> PaymentStatus.FAILED.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("REFUNDED 전이 (종단 상태)")
    class FromRefunded {

        @ParameterizedTest
        @DisplayName("REFUNDED에서 모든 상태 전이 불허")
        @EnumSource(PaymentStatus.class)
        void refundedIsTerminal(PaymentStatus target) {
            assertThatThrownBy(() -> PaymentStatus.REFUNDED.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }
}
