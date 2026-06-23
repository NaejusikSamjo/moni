package com.moni.payment.subscription.application.service;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.application.service.SubscriptionService;
import com.moni.payment.domain.port.LoadSubscriptionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService — GetSubscriptionStatus")
class GetSubscriptionStatusServiceTest {

    @Mock
    private LoadSubscriptionPort loadSubscriptionPort;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate NEXT_BILLING = LocalDate.now().plusMonths(1);

    private Subscription activeSubscription() {
        Instant now = Instant.now();
        return Subscription.reconstitute(
                UUID.randomUUID(),
                USER_ID,
                BillingKey.of("bk-test-001"),
                SubscriptionStatus.ACTIVE,
                NEXT_BILLING,
                now, now, 1L, List.of());
    }

    @Nested
    @DisplayName("getSubscriptionStatus()")
    class GetStatus {

        @Test
        @DisplayName("활성 구독이 존재하면 Subscription을 반환한다")
        void returnsActiveSubscription() {
            Subscription expected = activeSubscription();
            given(loadSubscriptionPort.findActiveByUserId(USER_ID))
                    .willReturn(Optional.of(expected));

            Subscription result = subscriptionService.getSubscriptionStatus(USER_ID);

            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getNextBillingDate()).isEqualTo(NEXT_BILLING);
        }

        @Test
        @DisplayName("활성 구독이 없으면 SUBSCRIPTION_NOT_FOUND 예외 발생")
        void throwsWhenNotFound() {
            given(loadSubscriptionPort.findActiveByUserId(USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.getSubscriptionStatus(USER_ID))
                    .isInstanceOf(PaymentException.class)
                    .extracting(e -> ((PaymentException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
    }
}
