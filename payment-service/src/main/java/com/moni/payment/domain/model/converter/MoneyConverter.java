package com.moni.payment.domain.model.converter;

import com.moni.payment.domain.model.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Money money) {
        return money == null ? null : money.getValue();
    }

    @Override
    public Money convertToEntityAttribute(BigDecimal value) {
        return value == null ? null : Money.of(value);
    }
}
