package com.moni.payment.subscription.application.service;

import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.domain.port.LoadSubscriptionPort;
import com.moni.payment.domain.port.SaveSubscriptionHistoryPort;
import com.moni.payment.domain.port.SaveSubscriptionPort;
import com.moni.payment.domain.port.SubscriptionEventPublisherPort;
import com.moni.payment.subscription.domain.port.in.ActivateSubscriptionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService — activateSubscription()")
class ActivateSubscriptionServiceTest {

    @Mock
    private LoadSubscriptionPort loadSubscriptionPort;
    @Mock
    private SaveSubscriptionPort saveSubscriptionPort;
    @Mock
    private SaveSubscriptionHistoryPort saveSubscriptionHistoryPort;
    @Mock
    private SubscriptionEventPublisherPort subscriptionEventPublisherPort;

    private SubscriptionService subscriptionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String BILLING_KEY_VALUE = "billing-key-001";
    private static final LocalDate NEXT_BILLING = LocalDate.now().plusMonths(1);

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                loadSubscriptionPort,
                saveSubscriptionPort,
                saveSubscriptionHistoryPort,
                subscriptionEventPublisherPort);
    }

    private ActivateSubscriptionUseCase.ActivateSubscriptionCommand command() {
        return new ActivateSubscriptionUseCase.ActivateSubscriptionCommand(
                USER_ID, BILLING_KEY_VALUE, NEXT_BILLING);
    }

    @Nested
    @DisplayName("활성화 성공")
    class Success {

        @BeforeEach
        void setUp() {
            given(saveSubscriptionPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("저장된 Subscription 상태가 ACTIVE이다")
        void subscriptionIsActive() {
            Subscription result = subscriptionService.activateSubscription(command());

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getBillingKey().getValue()).isEqualTo(BILLING_KEY_VALUE);
            assertThat(result.getNextBillingDate()).isEqualTo(NEXT_BILLING);
        }

        @Test
        @DisplayName("nextBillingDate가 오늘 + 1개월로 설정된다")
        void nextBillingDateIsNextMonth() {
            Subscription result = subscriptionService.activateSubscription(command());
            assertThat(result.getNextBillingDate()).isEqualTo(NEXT_BILLING);
        }

        @Test
        @DisplayName("SubscriptionHistory가 저장된다")
        void historyIsSaved() {
            subscriptionService.activateSubscription(command());
            then(saveSubscriptionHistoryPort).should().save(any(SubscriptionHistory.class));
        }

        @Test
        @DisplayName("SubscriptionActivatedEvent가 Kafka로 발행된다")
        void publishesActivatedEvent() {
            subscriptionService.activateSubscription(command());

            ArgumentCaptor<SubscriptionActivatedEvent> captor =
                    ArgumentCaptor.forClass(SubscriptionActivatedEvent.class);
            then(subscriptionEventPublisherPort).should().publishActivated(captor.capture());

            SubscriptionActivatedEvent event = captor.getValue();
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.billingKey().getValue()).isEqualTo(BILLING_KEY_VALUE);
        }
    }
}
