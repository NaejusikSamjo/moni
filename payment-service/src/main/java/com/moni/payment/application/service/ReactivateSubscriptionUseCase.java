package com.moni.payment.application.service;

import com.moni.payment.application.dto.command.ApprovePaymentCommand;
import com.moni.payment.application.dto.command.FailPaymentCommand;
import com.moni.payment.application.dto.command.RecordPendingPaymentCommand;
import com.moni.payment.application.dto.command.SubscribeResult;
import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.application.service.command.SubscriptionCommandService;
import com.moni.payment.application.service.command.SubscriptionQueryService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactivateSubscriptionUseCase {

    private final SubscriptionQueryService subscriptionQueryService;
    private final PaymentCommandService paymentCommandService;
    private final SubscriptionCommandService subscriptionCommandService;
    private final TossPaymentsAdapter tossPaymentsAdapter;

    public SubscribeResult execute(UUID userId) {
        Subscription suspended = subscriptionQueryService.findSuspendedSubscription(userId);

        if (suspended.getAmount() == null) {
            throw new PaymentException(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        UUID subscriptionId = suspended.getId();
        MerchantId merchantId = MerchantId.generate();
        Money amount = suspended.getAmount();
        String actor = userId.toString();

        UUID paymentId = paymentCommandService.recordPendingPayment(
                new RecordPendingPaymentCommand(
                        userId, merchantId, amount,
                        PaymentType.SUBSCRIPTION_REACTIVATION,
                        Instant.now().plusSeconds(600),
                        actor));

        TossPaymentsAdapter.PgPaymentResult pgResult;
        try {
            pgResult = tossPaymentsAdapter.requestBillingPayment(
                    suspended.getBillingKey().getValue(), amount, merchantId);
        } catch (Exception e) {
            paymentCommandService.failPayment(new FailPaymentCommand(paymentId, "PG_ERROR", actor));
            log.error("재활성화 PG 호출 실패: subscriptionId={}, paymentId={}", subscriptionId, paymentId, e);
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        if (!pgResult.success()) {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(paymentId, pgResult.rawResponse(), actor));
            log.warn("재활성화 PG 거절: subscriptionId={}, paymentId={}", subscriptionId, paymentId);
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        paymentCommandService.approvePayment(
                new ApprovePaymentCommand(
                        paymentId, pgResult.pgPaymentKey(), pgResult.billingKeyValue(),
                        pgResult.rawResponse(), actor));

        subscriptionCommandService.reactivateFromSuspended(subscriptionId);

        log.info("SUSPENDED 구독 재활성화 완료: subscriptionId={}, paymentId={}", subscriptionId, paymentId);
        return new SubscribeResult(paymentId, "COMPLETED", amount.getValue().longValue(),
                LocalDate.now().plusMonths(1));
    }
}
