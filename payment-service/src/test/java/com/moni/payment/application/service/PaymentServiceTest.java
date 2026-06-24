package com.moni.payment.application.service;

import com.moni.payment.application.command.ConfirmPaymentCommand;
import com.moni.payment.application.dto.GetPaymentHistoryQuery;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.infrastructure.repository.PaymentHistoryRepository;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock
    private PaymentJpaRepository paymentJpaRepository;
    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    private PaymentService paymentService;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentJpaRepository, paymentHistoryRepository);
    }

    @Nested
    @DisplayName("confirmPayment()")
    class ConfirmPayment {

        private static final String MERCHANT_ID_VALUE = "MONI" + "a".repeat(32);
        private static final String PG_PAYMENT_KEY = "toss_pg_confirm_001";
        private static final String PG_RESPONSE = "{\"status\":\"DONE\"}";
        private static final Instant RESPONDED_AT = Instant.now();

        private Payment pendingPayment() {
            return Payment.create(
                    USER_ID,
                    MerchantId.of(MERCHANT_ID_VALUE),
                    Money.of(9900L),
                    PaymentType.SUBSCRIPTION_INITIAL,
                    Instant.now().plusSeconds(600),
                    USER_ID.toString());
        }

        private ConfirmPaymentCommand confirmCommand() {
            return new ConfirmPaymentCommand(
                    MERCHANT_ID_VALUE, PG_PAYMENT_KEY, PG_RESPONSE, RESPONDED_AT, USER_ID.toString());
        }

        @Test
        @DisplayName("PENDING 결제 확인 성공 → COMPLETED 저장 후 히스토리 저장")
        void successCompletesPayment() {
            Payment payment = pendingPayment();
            given(paymentJpaRepository.findByMerchantId(MerchantId.of(MERCHANT_ID_VALUE)))
                    .willReturn(Optional.of(payment));
            given(paymentJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            paymentService.confirmPayment(confirmCommand());

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(paymentJpaRepository).should(times(1)).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            then(paymentHistoryRepository).should(times(1)).save(any());
        }

        @Test
        @DisplayName("merchantId에 해당하는 결제가 없으면 PAYMENT_NOT_FOUND 예외 발생")
        void throwsWhenPaymentNotFound() {
            given(paymentJpaRepository.findByMerchantId(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirmPayment(confirmCommand()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);

            then(paymentJpaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("이미 COMPLETED 상태인 결제 재확인 시 INVALID_PAYMENT_STATUS_TRANSITION 예외 발생")
        void idempotencyRejectsAlreadyCompleted() {
            Payment completed = Payment.reconstitute(
                    UUID.randomUUID(), MerchantId.of(MERCHANT_ID_VALUE), USER_ID,
                    PaymentType.SUBSCRIPTION_INITIAL, Money.of(9900L), PG_PAYMENT_KEY,
                    PaymentStatus.COMPLETED,
                    Instant.now().plusSeconds(600),
                    Instant.now(), USER_ID.toString(),
                    Instant.now(), USER_ID.toString(),
                    Collections.emptyList());
            given(paymentJpaRepository.findByMerchantId(any())).willReturn(Optional.of(completed));

            assertThatThrownBy(() -> paymentService.confirmPayment(confirmCommand()))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.INVALID_PAYMENT_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("userId에 해당하는 결제 목록을 반환한다")
        void returnsPaymentList() {
            Payment p1 = Payment.create(
                    USER_ID, MerchantId.of("MONI" + "b".repeat(32)),
                    Money.of(9900L), PaymentType.SUBSCRIPTION_INITIAL,
                    Instant.now().plusSeconds(600), USER_ID.toString());
            given(paymentJpaRepository.findByUserIdPaged(any(UUID.class), any(Pageable.class)))
                    .willReturn(List.of(p1));

            List<Payment> result = paymentService.getHistory(new GetPaymentHistoryQuery(USER_ID, 0, 10));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("결제 내역이 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoHistory() {
            given(paymentJpaRepository.findByUserIdPaged(any(UUID.class), any(Pageable.class)))
                    .willReturn(Collections.emptyList());

            List<Payment> result = paymentService.getHistory(new GetPaymentHistoryQuery(USER_ID, 0, 10));

            assertThat(result).isEmpty();
        }
    }
}
