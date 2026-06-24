package com.moni.payment.application.repository;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;

import java.util.UUID;

public interface PgGateway {

    record PgPaymentRequest(
            String authKey,
            MerchantId merchantId,
            UUID userId,
            Money amount,
            PaymentType paymentType) {
    }

    record PgPaymentResult(
            String pgPaymentKey,
            String billingKeyValue,
            String rawResponse,
            boolean success) {
    }

    enum PgPaymentStatus {
        APPROVED,
        CANCELED,
        FAILED,
        WAITING_FOR_DEPOSIT
    }

    PgPaymentResult requestPayment(PgPaymentRequest request);

    PgPaymentResult requestBillingPayment(String billingKeyValue, Money amount, MerchantId merchantId);

    PgPaymentStatus inquirePayment(String pgPaymentKey);
}
