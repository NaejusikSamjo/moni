package com.moni.payment.subscription.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;

import java.util.Objects;

public final class BillingKey {

    private final String value;

    private BillingKey(String value) {
        if (value == null || value.isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_BILLING_KEY);
        }
        this.value = value;
    }

    public static BillingKey of(String value) {
        return new BillingKey(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BillingKey that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
