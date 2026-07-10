package com.moni.payment.application.scheduler;

import com.moni.payment.application.dto.command.FailPaymentCommand;
import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendingPaymentExpireScheduler 테스트")
class PendingPaymentExpireSchedulerTest {

    @Mock private PaymentJpaRepository paymentJpaRepository;
    @Mock private PaymentCommandService paymentCommandService;

    @InjectMocks
    private PendingPaymentExpireScheduler pendingPaymentExpireScheduler;

    private Payment pendingPayment() {
        return Payment.create(
                UUID.randomUUID(),
                MerchantId.generate(),
                Money.of(new BigDecimal("9900")),
                PaymentType.SUBSCRIPTION_INITIAL,
                Instant.now().minusSeconds(60),
                "test-user");
    }

    @Nested
    @DisplayName("expireOverduePayments()")
    class ExpireOverduePayments {

        @Test
        void 만료_대상이_없으면_failPayment를_호출하지_않는다() {
            given(paymentJpaRepository.findByStatusAndExpiresAtBefore(
                    eq(PaymentStatus.PENDING), any(Instant.class)))
                    .willReturn(Collections.emptyList());

            pendingPaymentExpireScheduler.expireOverduePayments();

            then(paymentCommandService).should(never()).failPayment(any());
        }

        @Test
        void 만료된_PENDING_결제에_대해_failPayment를_호출한다() {
            Payment payment = pendingPayment();
            given(paymentJpaRepository.findByStatusAndExpiresAtBefore(
                    eq(PaymentStatus.PENDING), any(Instant.class)))
                    .willReturn(List.of(payment));

            pendingPaymentExpireScheduler.expireOverduePayments();

            ArgumentCaptor<FailPaymentCommand> captor = ArgumentCaptor.forClass(FailPaymentCommand.class);
            then(paymentCommandService).should().failPayment(captor.capture());
            assertThat(captor.getValue().paymentId()).isEqualTo(payment.getId());
        }

        @Test
        void failPayment_예외_발생_시_다음_결제를_계속_처리한다() {
            Payment first = pendingPayment();
            Payment second = pendingPayment();
            given(paymentJpaRepository.findByStatusAndExpiresAtBefore(
                    eq(PaymentStatus.PENDING), any(Instant.class)))
                    .willReturn(List.of(first, second));
            willThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND))
                    .given(paymentCommandService).failPayment(
                            argThatPaymentId(first.getId()));

            pendingPaymentExpireScheduler.expireOverduePayments();

            then(paymentCommandService).should(times(2)).failPayment(any());
        }

        @Test
        void 복수_만료_결제에_대해_각각_failPayment를_호출한다() {
            Payment first = pendingPayment();
            Payment second = pendingPayment();
            given(paymentJpaRepository.findByStatusAndExpiresAtBefore(
                    eq(PaymentStatus.PENDING), any(Instant.class)))
                    .willReturn(List.of(first, second));

            pendingPaymentExpireScheduler.expireOverduePayments();

            then(paymentCommandService).should(times(2)).failPayment(any());
        }

        private FailPaymentCommand argThatPaymentId(UUID paymentId) {
            return org.mockito.ArgumentMatchers.argThat(cmd -> paymentId.equals(cmd.paymentId()));
        }
    }
}
