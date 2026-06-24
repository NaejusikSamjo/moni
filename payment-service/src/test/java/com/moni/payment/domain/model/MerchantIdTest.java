package com.moni.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MerchantId VO")
class MerchantIdTest {

    @Nested
    @DisplayName("생성 성공")
    class ValidCreation {

        @Test
        @DisplayName("6자 최소 길이는 허용된다")
        void minLength() {
            MerchantId id = MerchantId.of("abc123");
            assertThat(id.getValue()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("64자 최대 길이는 허용된다")
        void maxLength() {
            String value = "a".repeat(64);
            assertThat(MerchantId.of(value).getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("영문 대소문자, 숫자, 하이픈, 언더스코어 모두 허용된다")
        void allAllowedChars() {
            MerchantId id = MerchantId.of("Moni-Pay_001");
            assertThat(id.getValue()).isEqualTo("Moni-Pay_001");
        }
    }

    @Nested
    @DisplayName("생성 실패")
    class InvalidCreation {

        @ParameterizedTest
        @DisplayName("5자 이하는 INVALID_MERCHANT_ID_FORMAT 예외를 던진다")
        @ValueSource(strings = {"", "a", "ab", "abc", "abcd", "abcde"})
        void tooShort(String value) {
            assertThatThrownBy(() -> MerchantId.of(value))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_MERCHANT_ID_FORMAT);
        }

        @Test
        @DisplayName("65자 이상은 INVALID_MERCHANT_ID_FORMAT 예외를 던진다")
        void tooLong() {
            String value = "a".repeat(65);
            assertThatThrownBy(() -> MerchantId.of(value))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_MERCHANT_ID_FORMAT);
        }

        @ParameterizedTest
        @DisplayName("허용되지 않는 특수문자는 예외를 던진다")
        @ValueSource(strings = {"abc@12", "abc 12", "abc.12", "abc#12", "abc/12"})
        void invalidChars(String value) {
            assertThatThrownBy(() -> MerchantId.of(value))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_MERCHANT_ID_FORMAT);
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 값은 동등하다")
        void sameValueEquals() {
            MerchantId a = MerchantId.of("moni-001");
            MerchantId b = MerchantId.of("moni-001");
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("다른 값은 동등하지 않다")
        void differentValueNotEqual() {
            assertThat(MerchantId.of("moni-001")).isNotEqualTo(MerchantId.of("moni-002"));
        }
    }
}
