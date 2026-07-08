package com.moni.portfolio.application.service;

import com.moni.portfolio.domain.entity.UserSubscriptionStatus;
import com.moni.portfolio.domain.enums.SubscriptionStatus;
import com.moni.portfolio.domain.repository.UserSubscriptionStatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("UserSubscriptionStatusCommandService 테스트")
@ExtendWith(MockitoExtension.class)
class UserSubscriptionStatusCommandServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID SUBSCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID UPDATED_SUBSCRIPTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-07T00:00:00Z");

    @Mock
    private UserSubscriptionStatusRepository userSubscriptionStatusRepository;

    @InjectMocks
    private UserSubscriptionStatusCommandService userSubscriptionStatusCommandService;

    @Nested
    @DisplayName("applyPaymentSubscriptionEvent()")
    class ApplyPaymentSubscriptionEvent {

        @Test
        @DisplayName("성공 - 기존 구독 상태가 없으면 신규 구독 상태를 저장한다")
        void success_create_subscription_status_when_status_not_exists() {
            // given
            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.empty());

            // when
            userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                    "SUBSCRIPTION_ACTIVATED",
                    SUBSCRIPTION_ID,
                    USER_ID,
                    OCCURRED_AT
            );

            // then
            ArgumentCaptor<UserSubscriptionStatus> statusCaptor =
                    ArgumentCaptor.forClass(UserSubscriptionStatus.class);
            then(userSubscriptionStatusRepository).should().save(statusCaptor.capture());

            UserSubscriptionStatus savedStatus = statusCaptor.getValue();
            assertThat(savedStatus.getUserId()).isEqualTo(USER_ID);
            assertThat(savedStatus.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(savedStatus.isSubscribed()).isTrue();
            assertThat(savedStatus.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(savedStatus.getLastEventType()).isEqualTo("SUBSCRIPTION_ACTIVATED");
            assertThat(savedStatus.getLastOccurredAt()).isEqualTo(OCCURRED_AT);
        }

        @Test
        @DisplayName("성공 - 기존 구독 상태가 있으면 최신 이벤트 상태로 갱신한다")
        void success_update_subscription_status_when_status_exists() {
            // given
            UserSubscriptionStatus subscriptionStatus = subscriptionStatus(
                    true,
                    SubscriptionStatus.ACTIVE,
                    "SUBSCRIPTION_ACTIVATED",
                    OCCURRED_AT
            );
            Instant updatedOccurredAt = Instant.parse("2026-07-07T01:00:00Z");

            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(subscriptionStatus));

            // when
            userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                    "SUBSCRIPTION_CANCELLED",
                    UPDATED_SUBSCRIPTION_ID,
                    USER_ID,
                    updatedOccurredAt
            );

            // then
            assertThat(subscriptionStatus.getSubscriptionId()).isEqualTo(UPDATED_SUBSCRIPTION_ID);
            assertThat(subscriptionStatus.isSubscribed()).isFalse();
            assertThat(subscriptionStatus.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(subscriptionStatus.getLastEventType()).isEqualTo("SUBSCRIPTION_CANCELLED");
            assertThat(subscriptionStatus.getLastOccurredAt()).isEqualTo(updatedOccurredAt);
            then(userSubscriptionStatusRepository).should(never()).save(any(UserSubscriptionStatus.class));
        }

        @Test
        @DisplayName("성공 - 구독 중지 이벤트를 받으면 SUSPENDED 상태로 갱신한다")
        void success_update_subscription_status_when_subscription_suspended() {
            // given
            UserSubscriptionStatus subscriptionStatus = subscriptionStatus(
                    true,
                    SubscriptionStatus.ACTIVE,
                    "SUBSCRIPTION_ACTIVATED",
                    OCCURRED_AT
            );
            Instant suspendedOccurredAt = Instant.parse("2026-07-07T01:00:00Z");

            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(subscriptionStatus));

            // when
            userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                    "SUBSCRIPTION_SUSPENDED",
                    UPDATED_SUBSCRIPTION_ID,
                    USER_ID,
                    suspendedOccurredAt
            );

            // then
            assertThat(subscriptionStatus.getSubscriptionId()).isEqualTo(UPDATED_SUBSCRIPTION_ID);
            assertThat(subscriptionStatus.isSubscribed()).isFalse();
            assertThat(subscriptionStatus.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
            assertThat(subscriptionStatus.getLastEventType()).isEqualTo("SUBSCRIPTION_SUSPENDED");
            assertThat(subscriptionStatus.getLastOccurredAt()).isEqualTo(suspendedOccurredAt);
            then(userSubscriptionStatusRepository).should(never()).save(any(UserSubscriptionStatus.class));
        }

        @Test
        @DisplayName("성공 - 필수 값이 누락되면 구독 상태를 조회하지 않는다")
        void success_ignore_when_required_value_missing() {
            // when
            userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                    null,
                    SUBSCRIPTION_ID,
                    USER_ID,
                    OCCURRED_AT
            );

            // then
            then(userSubscriptionStatusRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 - 알 수 없는 이벤트 타입이면 구독 상태를 조회하지 않는다")
        void success_ignore_when_event_type_unknown() {
            // when
            userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                    "UNKNOWN_EVENT",
                    SUBSCRIPTION_ID,
                    USER_ID,
                    OCCURRED_AT
            );

            // then
            then(userSubscriptionStatusRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 - 이미 반영된 이벤트보다 오래된 이벤트면 구독 상태를 갱신하지 않는다")
        void success_ignore_when_event_is_older_than_last_event() {
            // given
            Instant lastOccurredAt = Instant.parse("2026-07-07T01:00:00Z");
            UserSubscriptionStatus subscriptionStatus = subscriptionStatus(
                    true,
                    SubscriptionStatus.ACTIVE,
                    "SUBSCRIPTION_ACTIVATED",
                    lastOccurredAt
            );

            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(subscriptionStatus));

            // when
            userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                    "SUBSCRIPTION_CANCELLED",
                    UPDATED_SUBSCRIPTION_ID,
                    USER_ID,
                    OCCURRED_AT
            );

            // then
            assertThat(subscriptionStatus.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(subscriptionStatus.isSubscribed()).isTrue();
            assertThat(subscriptionStatus.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscriptionStatus.getLastEventType()).isEqualTo("SUBSCRIPTION_ACTIVATED");
            assertThat(subscriptionStatus.getLastOccurredAt()).isEqualTo(lastOccurredAt);
            then(userSubscriptionStatusRepository).should(never()).save(any(UserSubscriptionStatus.class));
        }

        @Test
        @DisplayName("성공 - 같은 이벤트가 중복 수신되어도 최종 구독 상태가 변하지 않는다")
        void success_keep_same_subscription_status_when_event_is_duplicated() {
            // given
            UserSubscriptionStatus subscriptionStatus = subscriptionStatus(
                    true,
                    SubscriptionStatus.ACTIVE,
                    "SUBSCRIPTION_ACTIVATED",
                    OCCURRED_AT
            );

            given(userSubscriptionStatusRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(subscriptionStatus));

            // when
            userSubscriptionStatusCommandService.applyPaymentSubscriptionEvent(
                    "SUBSCRIPTION_ACTIVATED",
                    SUBSCRIPTION_ID,
                    USER_ID,
                    OCCURRED_AT
            );

            // then
            assertThat(subscriptionStatus.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(subscriptionStatus.isSubscribed()).isTrue();
            assertThat(subscriptionStatus.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscriptionStatus.getLastEventType()).isEqualTo("SUBSCRIPTION_ACTIVATED");
            assertThat(subscriptionStatus.getLastOccurredAt()).isEqualTo(OCCURRED_AT);
            then(userSubscriptionStatusRepository).should(never()).save(any(UserSubscriptionStatus.class));
        }
    }

    private UserSubscriptionStatus subscriptionStatus(
            boolean subscribed,
            SubscriptionStatus status,
            String lastEventType,
            Instant lastOccurredAt
    ) {
        return UserSubscriptionStatus.create(
                USER_ID,
                SUBSCRIPTION_ID,
                subscribed,
                status,
                lastEventType,
                lastOccurredAt
        );
    }
}
