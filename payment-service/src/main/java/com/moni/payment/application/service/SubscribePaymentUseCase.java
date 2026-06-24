package com.moni.payment.application.service;

import com.moni.payment.application.command.ApprovePaymentCommand;
import com.moni.payment.application.command.FailPaymentCommand;
import com.moni.payment.application.command.RecordPendingPaymentCommand;
import com.moni.payment.application.command.SubscribeCommand;
import com.moni.payment.application.command.SubscribeResult;
import com.moni.payment.application.repository.PgPaymentClient;
import com.moni.payment.application.repository.SubscriptionLockRepository;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribePaymentUseCase {

    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final SubscriptionQueryService subscriptionQueryService;
    private final PaymentCommandService paymentCommandService;
    private final PgPaymentClient pgPaymentClient;
    private final SubscriptionLockRepository subscriptionLockRepository;

    public SubscribeResult execute(SubscribeCommand command) {
        UUID userId = command.userId();

        // [1] Redis 멱등키 — 중복 결제 방지
        if (!subscriptionLockRepository.tryLock(userId, LOCK_TTL)) {
            throw new PaymentException(PaymentErrorCode.DUPLICATE_PAYMENT);
        }

        try {
            // [2] 이미 활성 구독 여부 확인
            subscriptionQueryService.checkNoActiveSubscription(userId);

            // [3] Tx1: PENDING 저장
            MerchantId merchantId = MerchantId.generate();
            Money amount = Money.of(command.amount());
            UUID paymentId = paymentCommandService.recordPendingPayment(
                    new RecordPendingPaymentCommand(
                            userId, merchantId, amount,
                            PaymentType.SUBSCRIPTION_INITIAL,
                            Instant.now().plusSeconds(600),
                            command.requestedBy()));

            // [4] PG 호출 (트랜잭션 밖)
            PgPaymentClient.PgPaymentResult pgResult;
            try {
                pgResult = pgPaymentClient.requestPayment(
                        new PgPaymentClient.PgPaymentRequest(
                                command.authKey(), merchantId, userId, amount,
                                PaymentType.SUBSCRIPTION_INITIAL));
            } catch (Exception e) {
                // [5-Fail] PG 예외 → Tx2: FAILED 저장
                paymentCommandService.failPayment(
                        new FailPaymentCommand(paymentId, "PG_ERROR", command.requestedBy()));
                throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
            }

            if (!pgResult.success()) {
                // [5-Fail] PG 실패 → Tx2: FAILED 저장
                paymentCommandService.failPayment(
                        new FailPaymentCommand(paymentId, pgResult.rawResponse(), command.requestedBy()));
                throw new PaymentException(PaymentErrorCode.PG_PAYMENT_FAILED);
            }

            // [5-OK] Tx2: COMPLETED 저장 + PaymentCompletedEvent 발행 → Listener에서 Tx3 구독 활성화
            paymentCommandService.approvePayment(
                    new ApprovePaymentCommand(
                            paymentId, pgResult.pgPaymentKey(), pgResult.billingKeyValue(),
                            pgResult.rawResponse(), command.requestedBy()));

            log.info("구독 결제 완료: paymentId={}, userId={}", paymentId, userId);

            return new SubscribeResult(paymentId, "COMPLETED", amount.getValue().longValue(),
                    LocalDate.now().plusMonths(1));

        } finally {
            subscriptionLockRepository.unlock(userId);
        }
    }
}
