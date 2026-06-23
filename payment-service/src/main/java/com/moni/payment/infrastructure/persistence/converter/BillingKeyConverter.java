package com.moni.payment.infrastructure.persistence.converter;

import com.moni.payment.domain.model.BillingKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BillingKeyConverter implements AttributeConverter<BillingKey, String> {

    @Override
    public String convertToDatabaseColumn(BillingKey billingKey) {
        return billingKey == null ? null : billingKey.getValue();
    }

    @Override
    public BillingKey convertToEntityAttribute(String value) {
        return value == null ? null : BillingKey.of(value);
    }
}
