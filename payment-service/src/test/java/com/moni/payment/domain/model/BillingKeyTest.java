package com.moni.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BillingKey VO")
class BillingKeyTest {

    @Nested
    @DisplayName("생성 성공")
    class ValidCreation {

        @Test
        @DisplayName("유효한 빌링키 문자열로 생성된다")
        void validValue() {
            BillingKey key = BillingKey.of("billingkey-abc-123");
            assertThat(key.getValue()).isEqualTo("billingkey-abc-123");
        }
    }

    @Nested
    @DisplayName("생성 실패")
    class InvalidCreation {

        @Test
        @DisplayName("null은 INVALID_BILLING_KEY 예외를 던진다")
        void nullThrows() {
            assertThatThrownBy(() -> BillingKey.of(null))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_BILLING_KEY);
        }

        @Test
        @DisplayName("빈 문자열은 INVALID_BILLING_KEY 예외를 던진다")
        void emptyThrows() {
            assertThatThrownBy(() -> BillingKey.of(""))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_BILLING_KEY);
        }

        @Test
        @DisplayName("공백만 있는 문자열은 INVALID_BILLING_KEY 예외를 던진다")
        void blankThrows() {
            assertThatThrownBy(() -> BillingKey.of("   "))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_BILLING_KEY);
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 값은 동등하다")
        void sameValueEquals() {
            BillingKey a = BillingKey.of("bk-001");
            BillingKey b = BillingKey.of("bk-001");
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("다른 값은 동등하지 않다")
        void differentValueNotEqual() {
            assertThat(BillingKey.of("bk-001")).isNotEqualTo(BillingKey.of("bk-002"));
        }
    }

    @Test
    @DisplayName("toString은 빌링키 값 그대로이다")
    void toStringIsValue() {
        assertThat(BillingKey.of("bk-abc").toString()).isEqualTo("bk-abc");
    }
}
