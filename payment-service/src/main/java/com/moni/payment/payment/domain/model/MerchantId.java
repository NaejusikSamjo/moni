package com.moni.payment.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class MerchantId {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]{6,64}$");

    private final String value;

    private MerchantId(String value) {
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new PaymentException(PaymentErrorCode.INVALID_MERCHANT_ID_FORMAT);
        }
        this.value = value;
    }

    public static MerchantId of(String value) {
        return new MerchantId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MerchantId that)) {
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
