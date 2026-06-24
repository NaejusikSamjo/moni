package com.moni.payment.application.service;

import com.moni.common.response.paging.PageRes;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import com.moni.payment.presentation.dto.PaymentHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentJpaRepository paymentJpaRepository;

    @Transactional(readOnly = true)
    public PageRes<PaymentHistoryResponse> execute(UUID userId, Pageable pageable) {
        return new PageRes<>(
                paymentJpaRepository.findByUserIdPaged(userId, pageable)
                        .map(PaymentHistoryResponse::from));
    }
}
