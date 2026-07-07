package com.moni.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;

import java.util.EnumSet;
import java.util.Set;

public enum SubscriptionStatus {

    PENDING_ACTIVATION {
        @Override
        public Set<SubscriptionStatus> allowedNextStatuses() {
            return EnumSet.of(ACTIVE);
        }
    },
    ACTIVE {
        @Override
        public Set<SubscriptionStatus> allowedNextStatuses() {
            return EnumSet.of(CANCELLING, SUSPENDED);
        }
    },
    CANCELLING {
        @Override
        public Set<SubscriptionStatus> allowedNextStatuses() {
            return EnumSet.of(CANCELLED, ACTIVE);
        }
    },
    CANCELLED {
        @Override
        public Set<SubscriptionStatus> allowedNextStatuses() {
            return EnumSet.noneOf(SubscriptionStatus.class);
        }
    },
    SUSPENDED {
        @Override
        public Set<SubscriptionStatus> allowedNextStatuses() {
            return EnumSet.of(ACTIVE, CANCELLED);
        }
    };

    public abstract Set<SubscriptionStatus> allowedNextStatuses();

    public void validateTransitionTo(SubscriptionStatus next) {
        if (!allowedNextStatuses().contains(next)) {
            throw new PaymentException(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION);
        }
    }
}
