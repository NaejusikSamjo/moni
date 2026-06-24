package com.moni.payment.application.listener;

import com.moni.payment.application.dto.commandDto.ActivateSubscriptionCommand;
import com.moni.payment.application.service.SubscriptionCommandService;
import com.moni.payment.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionActivationListener {

    private final SubscriptionCommandService subscriptionCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신, 구독 활성화 시작: paymentId={}, userId={}",
                event.paymentId(), event.userId());

        subscriptionCommandService.activateSubscription(
                new ActivateSubscriptionCommand(event.userId(), event.billingKeyValue()));
    }
}
