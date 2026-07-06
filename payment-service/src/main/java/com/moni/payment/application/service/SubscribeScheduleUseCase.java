package com.moni.payment.application.service;

import com.moni.payment.application.dto.command.ApprovePaymentCommand;
import com.moni.payment.application.dto.command.FailPaymentCommand;
import com.moni.payment.application.dto.command.RecordPendingPaymentCommand;
import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.application.service.command.SubscriptionCommandService;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeScheduleUseCase {

    private static final String ACTOR = "system";

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final PaymentCommandService paymentCommandService;
    private final SubscriptionCommandService subscriptionCommandService;
    private final TossPaymentsAdapter tossPaymentsAdapter;

    public void execute() {
        List<Subscription> dueSubscriptions = subscriptionJpaRepository
                .findActiveSubscriptionsDueBefore(SubscriptionStatus.ACTIVE, LocalDate.now());

        log.info("정기결제 대상: {}건", dueSubscriptions.size());

        for (Subscription subscription : dueSubscriptions) {
            try {
                processRecurringPayment(subscription);
            } catch (Exception e) {
                log.error("정기결제 처리 중 예외 발생: subscriptionId={}", subscription.getId(), e);
            }
        }
    }

    private void processRecurringPayment(Subscription subscription) {
        if (subscription.getAmount() == null) {
            log.warn("정기결제 금액 정보 없음, 건너뜀: subscriptionId={}", subscription.getId());
            return;
        }

        UUID subscriptionId = subscription.getId();
        MerchantId merchantId = MerchantId.generate();

        UUID paymentId = paymentCommandService.recordPendingPayment(
                new RecordPendingPaymentCommand(
                        subscription.getUserId(), merchantId, subscription.getAmount(),
                        PaymentType.SUBSCRIPTION_RECURRING,
                        Instant.now().plusSeconds(600), ACTOR));

        TossPaymentsAdapter.PgPaymentResult pgResult;
        try {
            pgResult = tossPaymentsAdapter.requestBillingPayment(
                    subscription.getBillingKey().getValue(), subscription.getAmount(), merchantId);
        } catch (Exception e) {
            paymentCommandService.failPayment(new FailPaymentCommand(paymentId, "PG_ERROR", ACTOR));
            log.error("정기결제 PG 호출 실패: subscriptionId={}, paymentId={}", subscriptionId, paymentId, e);
            return;
        }

        if (!pgResult.success()) {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(paymentId, pgResult.rawResponse(), ACTOR));
            log.warn("정기결제 PG 거절: subscriptionId={}, paymentId={}", subscriptionId, paymentId);
            return;
        }

        paymentCommandService.approvePayment(
                new ApprovePaymentCommand(
                        paymentId, pgResult.pgPaymentKey(), pgResult.billingKeyValue(),
                        pgResult.rawResponse(), ACTOR));

        subscriptionCommandService.extendBillingDate(subscriptionId, LocalDate.now().plusMonths(1));

        log.info("정기결제 완료: subscriptionId={}, paymentId={}", subscriptionId, paymentId);
    }
}
