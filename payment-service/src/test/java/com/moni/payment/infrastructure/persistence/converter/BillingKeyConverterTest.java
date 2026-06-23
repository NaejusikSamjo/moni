package com.moni.payment.infrastructure.persistence.converter;

import com.moni.payment.domain.model.BillingKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BillingKeyConverter")
class BillingKeyConverterTest {

    private final BillingKeyConverter converter = new BillingKeyConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn()")
    class ConvertToDatabaseColumn {

        @Test
        @DisplayName("BillingKey를 String으로 변환한다")
        void convertsBillingKeyToString() {
            BillingKey billingKey = BillingKey.of("billing-key-001");
            String result = converter.convertToDatabaseColumn(billingKey);
            assertThat(result).isEqualTo("billing-key-001");
        }

        @Test
        @DisplayName("null BillingKey는 null을 반환한다")
        void returnsNullForNullBillingKey() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute()")
    class ConvertToEntityAttribute {

        @Test
        @DisplayName("String을 BillingKey로 변환한다")
        void convertsStringToBillingKey() {
            BillingKey result = converter.convertToEntityAttribute("billing-key-001");
            assertThat(result.getValue()).isEqualTo("billing-key-001");
        }

        @Test
        @DisplayName("null String은 null을 반환한다")
        void returnsNullForNullString() {
            assertThat(converter.convertToEntityAttribute(null)).isNull();
        }
    }
}
