package com.moni.payment.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;

import java.util.EnumSet;
import java.util.Set;

public enum PaymentStatus {

    PENDING {
        @Override
        public Set<PaymentStatus> allowedNextStatuses() {
            return EnumSet.of(COMPLETED, FAILED);
        }
    },
    COMPLETED {
        @Override
        public Set<PaymentStatus> allowedNextStatuses() {
            return EnumSet.of(REFUNDED);
        }
    },
    FAILED {
        @Override
        public Set<PaymentStatus> allowedNextStatuses() {
            return EnumSet.noneOf(PaymentStatus.class);
        }
    },
    REFUNDED {
        @Override
        public Set<PaymentStatus> allowedNextStatuses() {
            return EnumSet.noneOf(PaymentStatus.class);
        }
    };

    public abstract Set<PaymentStatus> allowedNextStatuses();

    public void validateTransitionTo(PaymentStatus next) {
        if (!allowedNextStatuses().contains(next)) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }
}
