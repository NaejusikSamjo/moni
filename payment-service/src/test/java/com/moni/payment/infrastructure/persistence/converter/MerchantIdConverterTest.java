package com.moni.payment.infrastructure.persistence.converter;

import com.moni.payment.domain.model.MerchantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MerchantIdConverter")
class MerchantIdConverterTest {

    private final MerchantIdConverter converter = new MerchantIdConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn()")
    class ConvertToDatabaseColumn {

        @Test
        @DisplayName("MerchantId를 String으로 변환한다")
        void convertsMerchantIdToString() {
            MerchantId merchantId = MerchantId.of("order-001");
            String result = converter.convertToDatabaseColumn(merchantId);
            assertThat(result).isEqualTo("order-001");
        }

        @Test
        @DisplayName("null MerchantId는 null을 반환한다")
        void returnsNullForNullMerchantId() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute()")
    class ConvertToEntityAttribute {

        @Test
        @DisplayName("String을 MerchantId로 변환한다")
        void convertsStringToMerchantId() {
            MerchantId result = converter.convertToEntityAttribute("order-001");
            assertThat(result.getValue()).isEqualTo("order-001");
        }

        @Test
        @DisplayName("null String은 null을 반환한다")
        void returnsNullForNullString() {
            assertThat(converter.convertToEntityAttribute(null)).isNull();
        }
    }
}
