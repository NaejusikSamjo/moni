package com.moni.payment.application.service;

import com.moni.payment.application.command.ConfirmPaymentCommand;
import com.moni.payment.application.dto.GetPaymentHistoryQuery;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.infrastructure.repository.PaymentHistoryRepository;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    @Transactional
    public void confirmPayment(ConfirmPaymentCommand command) {
        Payment payment = paymentJpaRepository.findByMerchantId(MerchantId.of(command.merchantId()))
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        payment.complete(command.pgPaymentKey(), null, command.pgResponse(), command.respondedAt(), command.confirmedBy());
        paymentJpaRepository.save(payment);
        paymentHistoryRepository.save(payment.pullLatestHistory());
        log.info("결제 CONFIRM 저장: paymentId={}", payment.getId());
    }

    @Transactional(readOnly = true)
    public List<Payment> getHistory(GetPaymentHistoryQuery query) {
        return paymentJpaRepository.findByUserIdPaged(query.userId(), PageRequest.of(query.page(), query.size()));
    }
}
