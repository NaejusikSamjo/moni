package com.moni.payment.application.service;

import com.moni.payment.application.command.ApprovePaymentCommand;
import com.moni.payment.application.command.FailPaymentCommand;
import com.moni.payment.application.command.RecordPendingPaymentCommand;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.PaymentCompletedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandService")
class PaymentCommandServiceTest {

    @Mock
    private PaymentJpaRepository paymentJpaRepository;
    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private PaymentCommandService paymentCommandService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PG_PAYMENT_KEY = "toss_pg_key_001";
    private static final String BILLING_KEY = "bk-001";
    private static final String ACTOR = "user-system";

    @BeforeEach
    void setUp() {
        paymentCommandService = new PaymentCommandService(
                paymentJpaRepository, paymentHistoryRepository, applicationEventPublisher);
    }

    private Payment pendingPayment() {
        return Payment.create(USER_ID, MerchantId.generate(), Money.of(9900L),
                PaymentType.SUBSCRIPTION_INITIAL, Instant.now().plusSeconds(600), ACTOR);
    }

    @Nested
    @DisplayName("recordPendingPayment()")
    class RecordPendingPayment {

        @Test
        @DisplayName("Payment를 PENDING 상태로 저장하고 paymentId를 반환한다")
        void savesPendingPaymentAndReturnsId() {
            given(paymentJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RecordPendingPaymentCommand command = new RecordPendingPaymentCommand(
                    USER_ID, MerchantId.generate(), Money.of(9900L),
                    PaymentType.SUBSCRIPTION_INITIAL, Instant.now().plusSeconds(600), ACTOR);

            UUID paymentId = paymentCommandService.recordPendingPayment(command);

            assertThat(paymentId).isNotNull();
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(paymentJpaRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("approvePayment()")
    class ApprovePayment {

        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = pendingPayment();
            payment.pullDomainEvents();
        }

        @Test
        @DisplayName("Payment가 COMPLETED 상태로 저장된다")
        void savesCompletedPayment() {
            given(paymentJpaRepository.findById(payment.getId())).willReturn(Optional.of(payment));
            given(paymentJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            paymentCommandService.approvePayment(
                    new ApprovePaymentCommand(payment.getId(), PG_PAYMENT_KEY, BILLING_KEY, "{}", ACTOR));

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(paymentJpaRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("PaymentHistory가 저장된다")
        void savesHistory() {
            given(paymentJpaRepository.findById(payment.getId())).willReturn(Optional.of(payment));
            given(paymentJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            paymentCommandService.approvePayment(
                    new ApprovePaymentCommand(payment.getId(), PG_PAYMENT_KEY, BILLING_KEY, "{}", ACTOR));

            then(paymentHistoryRepository).should().save(any());
        }

        @Test
        @DisplayName("PaymentCompletedEvent가 ApplicationEventPublisher를 통해 발행된다")
        void publishesPaymentCompletedEvent() {
            given(paymentJpaRepository.findById(payment.getId())).willReturn(Optional.of(payment));
            given(paymentJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            paymentCommandService.approvePayment(
                    new ApprovePaymentCommand(payment.getId(), PG_PAYMENT_KEY, BILLING_KEY, "{}", ACTOR));

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            then(applicationEventPublisher).should().publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(PaymentCompletedEvent.class);

            PaymentCompletedEvent event = (PaymentCompletedEvent) captor.getValue();
            assertThat(event.billingKeyValue()).isEqualTo(BILLING_KEY);
            assertThat(event.pgPaymentKey()).isEqualTo(PG_PAYMENT_KEY);
        }

        @Test
        @DisplayName("paymentId에 해당하는 결제가 없으면 PAYMENT_NOT_FOUND 예외 발생")
        void throwsWhenPaymentNotFound() {
            UUID unknownId = UUID.randomUUID();
            given(paymentJpaRepository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentCommandService.approvePayment(
                    new ApprovePaymentCommand(unknownId, PG_PAYMENT_KEY, BILLING_KEY, "{}", ACTOR)))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);

            then(applicationEventPublisher).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("failPayment()")
    class FailPayment {

        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = pendingPayment();
            given(paymentJpaRepository.findById(payment.getId())).willReturn(Optional.of(payment));
            given(paymentJpaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Payment가 FAILED 상태로 저장된다")
        void savesFailedPayment() {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(payment.getId(), "{\"error\":\"CARD_LIMIT\"}", ACTOR));

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            then(paymentJpaRepository).should().save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("PaymentHistory가 저장된다")
        void savesHistory() {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(payment.getId(), "{}", ACTOR));

            then(paymentHistoryRepository).should().save(any());
        }

        @Test
        @DisplayName("FAILED 처리 시 ApplicationEvent는 발행되지 않는다")
        void doesNotPublishEventOnFailure() {
            paymentCommandService.failPayment(
                    new FailPaymentCommand(payment.getId(), "{}", ACTOR));

            then(applicationEventPublisher).should(never()).publishEvent(any());
        }
    }
}
