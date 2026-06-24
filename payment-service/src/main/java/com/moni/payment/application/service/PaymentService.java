package com.moni.payment.application.service;

import com.moni.payment.application.repository.PaymentRepository;
import com.moni.payment.application.usecase.ConfirmPaymentUseCase;
import com.moni.payment.application.usecase.GetPaymentHistoryUseCase;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService implements ConfirmPaymentUseCase, GetPaymentHistoryUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void confirmPayment(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findByMerchantId(MerchantId.of(command.merchantId()))
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        payment.complete(command.pgPaymentKey(), null, command.pgResponse(), command.respondedAt(), command.confirmedBy());
        paymentRepository.save(payment);
        paymentRepository.saveHistory(payment.pullLatestHistory());
        log.info("결제 CONFIRM 저장: paymentId={}", payment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getHistory(GetPaymentHistoryQuery query) {
        return paymentRepository.findByUserId(query.userId(), query.page(), query.size());
    }
}
