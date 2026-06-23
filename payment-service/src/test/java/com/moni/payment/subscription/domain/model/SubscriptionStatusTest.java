package com.moni.payment.subscription.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SubscriptionStatus 상태 머신")
class SubscriptionStatusTest {

    @Nested
    @DisplayName("PENDING_ACTIVATION 전이")
    class FromPendingActivation {

        @Test
        @DisplayName("PENDING_ACTIVATION → ACTIVE 허용")
        void toActive() {
            assertThatCode(() -> SubscriptionStatus.PENDING_ACTIVATION
                    .validateTransitionTo(SubscriptionStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("PENDING_ACTIVATION → PENDING_ACTIVATION/CANCELLING/CANCELLED/SUSPENDED 불허")
        @EnumSource(value = SubscriptionStatus.class,
                names = {"PENDING_ACTIVATION", "CANCELLING", "CANCELLED", "SUSPENDED"})
        void toInvalidThrows(SubscriptionStatus target) {
            assertThatThrownBy(() -> SubscriptionStatus.PENDING_ACTIVATION.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("ACTIVE 전이")
    class FromActive {

        @Test
        @DisplayName("ACTIVE → CANCELLING 허용")
        void toCancelling() {
            assertThatCode(() -> SubscriptionStatus.ACTIVE
                    .validateTransitionTo(SubscriptionStatus.CANCELLING))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → SUSPENDED 허용")
        void toSuspended() {
            assertThatCode(() -> SubscriptionStatus.ACTIVE
                    .validateTransitionTo(SubscriptionStatus.SUSPENDED))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("ACTIVE → PENDING_ACTIVATION/ACTIVE/CANCELLED 불허")
        @EnumSource(value = SubscriptionStatus.class,
                names = {"PENDING_ACTIVATION", "ACTIVE", "CANCELLED"})
        void toInvalidThrows(SubscriptionStatus target) {
            assertThatThrownBy(() -> SubscriptionStatus.ACTIVE.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("CANCELLING 전이")
    class FromCancelling {

        @Test
        @DisplayName("CANCELLING → CANCELLED 허용")
        void toCancelled() {
            assertThatCode(() -> SubscriptionStatus.CANCELLING
                    .validateTransitionTo(SubscriptionStatus.CANCELLED))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("CANCELLING → PENDING_ACTIVATION/ACTIVE/CANCELLING/SUSPENDED 불허")
        @EnumSource(value = SubscriptionStatus.class,
                names = {"PENDING_ACTIVATION", "ACTIVE", "CANCELLING", "SUSPENDED"})
        void toInvalidThrows(SubscriptionStatus target) {
            assertThatThrownBy(() -> SubscriptionStatus.CANCELLING.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("CANCELLED 전이 (종단 상태)")
    class FromCancelled {

        @ParameterizedTest
        @DisplayName("CANCELLED에서 모든 상태 전이 불허")
        @EnumSource(SubscriptionStatus.class)
        void cancelledIsTerminal(SubscriptionStatus target) {
            assertThatThrownBy(() -> SubscriptionStatus.CANCELLED.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("SUSPENDED 전이")
    class FromSuspended {

        @Test
        @DisplayName("SUSPENDED → ACTIVE 허용")
        void toActive() {
            assertThatCode(() -> SubscriptionStatus.SUSPENDED
                    .validateTransitionTo(SubscriptionStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED → CANCELLED 허용")
        void toCancelled() {
            assertThatCode(() -> SubscriptionStatus.SUSPENDED
                    .validateTransitionTo(SubscriptionStatus.CANCELLED))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("SUSPENDED → PENDING_ACTIVATION/CANCELLING/SUSPENDED 불허")
        @EnumSource(value = SubscriptionStatus.class,
                names = {"PENDING_ACTIVATION", "CANCELLING", "SUSPENDED"})
        void toInvalidThrows(SubscriptionStatus target) {
            assertThatThrownBy(() -> SubscriptionStatus.SUSPENDED.validateTransitionTo(target))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION);
        }
    }
}
