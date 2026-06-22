package com.moni.payment.subscription.application.service;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.subscription.domain.model.Subscription;
import com.moni.payment.subscription.domain.port.in.GetSubscriptionStatusUseCase;
import com.moni.payment.subscription.domain.port.out.LoadSubscriptionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements GetSubscriptionStatusUseCase {

    private final LoadSubscriptionPort loadSubscriptionPort;

    @Override
    @Transactional(readOnly = true)
    public Subscription getSubscriptionStatus(UUID userId) {
        return loadSubscriptionPort.findActiveByUserId(userId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
