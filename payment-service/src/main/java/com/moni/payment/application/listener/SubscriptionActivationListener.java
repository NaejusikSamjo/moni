package com.moni.payment.application.listener;

import com.moni.payment.application.dto.command.ActivateSubscriptionCommand;
import com.moni.payment.application.service.command.SubscriptionCommandService;
import com.moni.payment.application.service.command.SubscriptionQueryService;
import com.moni.payment.domain.event.PaymentCompletedEvent;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionActivationListener {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신, 구독 활성화 시작: paymentId={}, userId={}",
                event.paymentId(), event.userId());

        Optional<Subscription> cancellingSubscription =
                subscriptionQueryService.findCancellingSubscription(event.userId());

        if (cancellingSubscription.isPresent()) {
            subscriptionCommandService.reactivateSubscription(
                    cancellingSubscription.get().getId(),
                    BillingKey.of(event.billingKeyValue()));
        } else {
            subscriptionCommandService.activateSubscription(
                    new ActivateSubscriptionCommand(event.userId(), event.billingKeyValue()));
        }
    }
}
