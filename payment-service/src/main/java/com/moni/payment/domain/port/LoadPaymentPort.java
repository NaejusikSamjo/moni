package com.moni.payment.domain.port;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPaymentPort {

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByMerchantId(MerchantId merchantId);

    List<Payment> findByUserId(UUID userId, int page, int size);
}
