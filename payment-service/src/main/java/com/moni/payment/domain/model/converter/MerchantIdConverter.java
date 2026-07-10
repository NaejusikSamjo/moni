package com.moni.payment.domain.model.converter;

import com.moni.payment.domain.model.MerchantId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MerchantIdConverter implements AttributeConverter<MerchantId, String> {

    @Override
    public String convertToDatabaseColumn(MerchantId merchantId) {
        return merchantId == null ? null : merchantId.getValue();
    }

    @Override
    public MerchantId convertToEntityAttribute(String value) {
        return value == null ? null : MerchantId.of(value);
    }
}
