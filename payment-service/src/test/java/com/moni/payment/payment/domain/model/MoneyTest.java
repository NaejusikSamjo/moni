package com.moni.payment.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money VO")
class MoneyTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("0원은 허용한다")
        void zeroIsAllowed() {
            Money money = Money.of(0L);
            assertThat(money.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("양수 금액으로 생성된다")
        void positiveAmount() {
            Money money = Money.of(9900L);
            assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("9900"));
        }

        @Test
        @DisplayName("BigDecimal로도 생성된다")
        void fromBigDecimal() {
            Money money = Money.of(new BigDecimal("9900.50"));
            assertThat(money.getValue()).isEqualByComparingTo(new BigDecimal("9900.50"));
        }

        @Test
        @DisplayName("음수 금액은 NEGATIVE_AMOUNT 예외를 던진다")
        void negativeThrows() {
            assertThatThrownBy(() -> Money.of(-1L))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.NEGATIVE_AMOUNT);
        }

        @Test
        @DisplayName("음수 BigDecimal도 NEGATIVE_AMOUNT 예외를 던진다")
        void negativeBigDecimalThrows() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("-0.01")))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.NEGATIVE_AMOUNT);
        }
    }

    @Nested
    @DisplayName("덧셈")
    class Addition {

        @Test
        @DisplayName("두 Money를 더한 결과는 합산 금액이다")
        void addReturnsSum() {
            Money a = Money.of(3000L);
            Money b = Money.of(6900L);
            assertThat(a.add(b)).isEqualTo(Money.of(9900L));
        }

        @Test
        @DisplayName("0과 더해도 원래 금액이다")
        void addZeroIsIdentity() {
            Money money = Money.of(5000L);
            assertThat(money.add(Money.of(0L))).isEqualTo(money);
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("소수점 trailing zero가 달라도 같은 금액으로 취급한다")
        void trailingZeroEquality() {
            Money a = Money.of(new BigDecimal("9900.00"));
            Money b = Money.of(new BigDecimal("9900"));
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("금액이 다르면 다른 객체이다")
        void differentAmountNotEqual() {
            assertThat(Money.of(1000L)).isNotEqualTo(Money.of(2000L));
        }
    }

    @Test
    @DisplayName("toString은 plain string 형태이다")
    void toStringIsPlainString() {
        assertThat(Money.of(9900L).toString()).isEqualTo("9900");
    }
}
