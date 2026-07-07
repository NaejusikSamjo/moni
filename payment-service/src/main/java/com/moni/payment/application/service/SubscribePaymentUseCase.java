package com.moni.payment.application.service;

import com.moni.payment.application.dto.command.ApprovePaymentCommand;
import com.moni.payment.application.dto.command.FailPaymentCommand;
import com.moni.payment.application.dto.command.RecordPendingPaymentCommand;
import com.moni.payment.application.dto.command.SubscribeCommand;
import com.moni.payment.application.dto.command.SubscribeResult;
import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.application.service.command.SubscriptionQueryService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import com.moni.payment.infrastructure.lock.RedisSubscriptionLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribePaymentUseCase {

    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final SubscriptionQueryService subscriptionQueryService;
    private final PaymentCommandService paymentCommandService;
    private final TossPaymentsAdapter tossPaymentsAdapter;
    private final RedisSubscriptionLock redisSubscriptionLock;

    public SubscribeResult execute(SubscribeCommand command) {
        UUID userId = command.userId();

        if (!redisSubscriptionLock.tryLock(userId, LOCK_TTL)) {
            throw new PaymentException(PaymentErrorCode.DUPLICATE_PAYMENT);
        }

        try {
            subscriptionQueryService.checkNoActiveSubscription(userId);

            Optional<Subscription> cancellingSubscription =
                    subscriptionQueryService.findCancellingSubscription(userId);

          return cancellingSubscription.map(
                  subscription -> executeReactivation(command, subscription))
              .orElseGet(() -> executeNewSubscription(command));

        } finally {
            redisSubscriptionLock.unlock(userId);
        }
    }

    private SubscribeResult executeNewSubscription(SubscribeCommand command) {
        UUID userId = command.userId();
        MerchantId merchantId = MerchantId.generate();
        Money amount = Money.of(command.amount());

        UUID paymentId = paymentCommandService.recordPendingPayment(
                new RecordPendingPaymentCommand(
                        userId, merchantId, amount,
                        PaymentType.SUBSCRIPTION_INITIAL,
                        Instant.now().plusSeconds(600),
                        command.requestedBy()));

        TossPaymentsAdapter.PgPaymentResult pgResult;
        try {
            pgResult = tossPaymentsAdapter.requestPayment(
                    new TossPaymentsAdapter.PgPaymentRequest(
                            command.authKey(), command.customerKey(),
                            merchantId, userId, amount,
                            PaymentType.SUBSCRIPTION_INITIAL));
        } catch (Exception e) {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(paymentId, "PG_ERROR", command.requestedBy()));
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        if (!pgResult.success()) {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(paymentId, pgResult.rawResponse(), command.requestedBy()));
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        paymentCommandService.approvePayment(
                new ApprovePaymentCommand(
                        paymentId, pgResult.pgPaymentKey(), pgResult.billingKeyValue(),
                        pgResult.rawResponse(), command.requestedBy()));

        log.info("신규 구독 결제 완료: paymentId={}, userId={}", paymentId, userId);
        return new SubscribeResult(paymentId, "COMPLETED", amount.getValue().longValue(),
                LocalDate.now().plusMonths(1));
    }

    private SubscribeResult executeReactivation(SubscribeCommand command, Subscription cancellingSubscription) {
        UUID userId = command.userId();
        String existingBillingKey = cancellingSubscription.getBillingKey().getValue();
        MerchantId merchantId = MerchantId.generate();
        Money amount = Money.of(command.amount());

        UUID paymentId = paymentCommandService.recordPendingPayment(
                new RecordPendingPaymentCommand(
                        userId, merchantId, amount,
                        PaymentType.SUBSCRIPTION_REACTIVATION,
                        Instant.now().plusSeconds(600),
                        command.requestedBy()));

        TossPaymentsAdapter.PgPaymentResult pgResult;
        try {
            pgResult = tossPaymentsAdapter.requestBillingPayment(existingBillingKey, amount, merchantId);
        } catch (Exception e) {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(paymentId, "PG_ERROR", command.requestedBy()));
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        if (!pgResult.success()) {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(paymentId, pgResult.rawResponse(), command.requestedBy()));
            throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
        }

        paymentCommandService.approvePayment(
                new ApprovePaymentCommand(
                        paymentId, pgResult.pgPaymentKey(), pgResult.billingKeyValue(),
                        pgResult.rawResponse(), command.requestedBy()));

        log.info("재구독 결제 완료: paymentId={}, userId={}, subscriptionId={}",
                paymentId, userId, cancellingSubscription.getId());
        return new SubscribeResult(paymentId, "COMPLETED", amount.getValue().longValue(),
                LocalDate.now().plusMonths(1));
    }
}
