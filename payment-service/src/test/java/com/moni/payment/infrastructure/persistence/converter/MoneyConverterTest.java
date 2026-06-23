package com.moni.payment.infrastructure.persistence.converter;

import com.moni.payment.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoneyConverter")
class MoneyConverterTest {

    private final MoneyConverter converter = new MoneyConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn()")
    class ConvertToDatabaseColumn {

        @Test
        @DisplayName("Money를 BigDecimal로 변환한다")
        void convertsMoneyToBigDecimal() {
            Money money = Money.of(9900L);
            BigDecimal result = converter.convertToDatabaseColumn(money);
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(9900));
        }

        @Test
        @DisplayName("null Money는 null을 반환한다")
        void returnsNullForNullMoney() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute()")
    class ConvertToEntityAttribute {

        @Test
        @DisplayName("BigDecimal을 Money로 변환한다")
        void convertsBigDecimalToMoney() {
            BigDecimal value = BigDecimal.valueOf(9900);
            Money result = converter.convertToEntityAttribute(value);
            assertThat(result).isEqualTo(Money.of(9900L));
        }

        @Test
        @DisplayName("null BigDecimal은 null을 반환한다")
        void returnsNullForNullBigDecimal() {
            assertThat(converter.convertToEntityAttribute(null)).isNull();
        }
    }
}
