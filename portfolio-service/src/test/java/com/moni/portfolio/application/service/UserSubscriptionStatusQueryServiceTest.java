package com.moni.portfolio.application.service;

import com.moni.portfolio.domain.entity.UserSubscriptionStatus;
import com.moni.portfolio.domain.enums.SubscriptionStatus;
import com.moni.portfolio.domain.repository.UserSubscriptionStatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DisplayName("UserSubscriptionStatusQueryService 테스트")
@ExtendWith(MockitoExtension.class)
class UserSubscriptionStatusQueryServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID SUBSCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Mock
    private UserSubscriptionStatusRepository userSubscriptionStatusRepository;

    @InjectMocks
    private UserSubscriptionStatusQueryService userSubscriptionStatusQueryService;

    @Nested
    @DisplayName("isPaidPlan()")
    class IsPaidPlan {

        @Test
        @DisplayName("성공 - 로컬 구독 상태가 없으면 무료 플랜으로 판단한다")
        void success_missing_subscription_status_returns_free_plan() {
            // given
            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.empty());

            // when
            boolean result = userSubscriptionStatusQueryService.isPaidPlan(USER_ID);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("성공 - ACTIVE 구독 상태면 유료 플랜으로 판단한다")
        void success_active_subscription_returns_paid_plan() {
            // given
            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(subscriptionStatus(true, SubscriptionStatus.ACTIVE)));

            // when
            boolean result = userSubscriptionStatusQueryService.isPaidPlan(USER_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공 - CANCELLING 구독 상태면 유료 플랜으로 판단한다")
        void success_cancelling_subscription_returns_paid_plan() {
            // given
            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(subscriptionStatus(true, SubscriptionStatus.CANCELLING)));

            // when
            boolean result = userSubscriptionStatusQueryService.isPaidPlan(USER_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("성공 - CANCELLED 구독 상태면 무료 플랜으로 판단한다")
        void success_cancelled_subscription_returns_free_plan() {
            // given
            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(subscriptionStatus(false, SubscriptionStatus.CANCELLED)));

            // when
            boolean result = userSubscriptionStatusQueryService.isPaidPlan(USER_ID);

            // then
            assertThat(result).isFalse();
        }
    }

    private UserSubscriptionStatus subscriptionStatus(boolean subscribed, SubscriptionStatus status) {
        return UserSubscriptionStatus.create(
                USER_ID,
                SUBSCRIPTION_ID,
                subscribed,
                status,
                "SUBSCRIPTION_ACTIVATED",
                Instant.parse("2026-07-07T00:00:00Z")
        );
    }
}
