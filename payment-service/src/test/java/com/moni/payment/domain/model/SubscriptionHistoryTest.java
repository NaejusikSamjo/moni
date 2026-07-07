package com.moni.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubscriptionHistory 도메인 테스트")
class SubscriptionHistoryTest {

    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

    @Nested
    @DisplayName("SubscriptionHistory.of()")
    class Of {

        @Test
        void 생성_시_subscriptionId와_상태_전이_정보가_설정된다() {
            SubscriptionHistory history = SubscriptionHistory.of(
                    SUBSCRIPTION_ID,
                    SubscriptionStatus.PENDING_ACTIVATION,
                    SubscriptionStatus.ACTIVE,
                    "최초 결제 성공");

            assertThat(history.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(history.getFromStatus()).isEqualTo(SubscriptionStatus.PENDING_ACTIVATION);
            assertThat(history.getToStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void 생성_시_reason이_설정된다() {
            String reason = "사용자 요청";

            SubscriptionHistory history = SubscriptionHistory.of(
                    SUBSCRIPTION_ID,
                    SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.CANCELLING,
                    reason);

            assertThat(history.getReason()).isEqualTo(reason);
        }

        @Test
        void 생성_시_id가_자동으로_부여된다() {
            SubscriptionHistory history = SubscriptionHistory.of(
                    SUBSCRIPTION_ID,
                    SubscriptionStatus.PENDING_ACTIVATION,
                    SubscriptionStatus.ACTIVE,
                    "최초 결제 성공");

            assertThat(history.getId()).isNotNull();
        }

        @Test
        void 생성_시_changedAt이_null이_아니다() {
            SubscriptionHistory history = SubscriptionHistory.of(
                    SUBSCRIPTION_ID,
                    SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.SUSPENDED,
                    "결제 실패");

            assertThat(history.getChangedAt()).isNotNull();
        }

        @Test
        void 두_번_생성하면_서로_다른_id를_가진다() {
            SubscriptionHistory history1 = SubscriptionHistory.of(
                    SUBSCRIPTION_ID, SubscriptionStatus.PENDING_ACTIVATION,
                    SubscriptionStatus.ACTIVE, "첫 번째");
            SubscriptionHistory history2 = SubscriptionHistory.of(
                    SUBSCRIPTION_ID, SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.CANCELLING, "두 번째");

            assertThat(history1.getId()).isNotEqualTo(history2.getId());
        }
    }
}
