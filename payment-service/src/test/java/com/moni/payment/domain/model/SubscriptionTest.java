package com.moni.payment.domain.model;

import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Subscription Aggregate")
class SubscriptionTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate NEXT_BILLING = LocalDate.now().plusMonths(1);
    private static final BillingKey BILLING_KEY = BillingKey.of("bk-toss-001");

    private Subscription subscription;

    @BeforeEach
    void setUp() {
        subscription = Subscription.create(USER_ID);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("생성 직후 상태는 PENDING_ACTIVATION이다")
        void statusIsPendingActivation() {
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_ACTIVATION);
        }

        @Test
        @DisplayName("생성 직후 billingKey는 null이다")
        void billingKeyIsNull() {
            assertThat(subscription.getBillingKey()).isNull();
        }

        @Test
        @DisplayName("생성 직후 histories는 비어있다")
        void historiesIsEmpty() {
            assertThat(subscription.getHistories()).isEmpty();
        }
    }

    @Nested
    @DisplayName("activate()")
    class Activate {

        @Test
        @DisplayName("PENDING_ACTIVATION → ACTIVE 전이 후 상태가 변경된다")
        void statusBecomesActive() {
            subscription.activate(BILLING_KEY);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("activate() 후 billingKey가 설정된다")
        void billingKeyIsSet() {
            subscription.activate(BILLING_KEY);
            assertThat(subscription.getBillingKey()).isEqualTo(BILLING_KEY);
        }

        @Test
        @DisplayName("activate() 후 SubscriptionActivatedEvent가 발행된다")
        void emitsActivatedEvent() {
            subscription.activate(BILLING_KEY);

            List<Object> events = subscription.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(SubscriptionActivatedEvent.class);

            SubscriptionActivatedEvent event = (SubscriptionActivatedEvent) events.get(0);
            assertThat(event.subscriptionId()).isEqualTo(subscription.getId());
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.billingKey()).isEqualTo(BILLING_KEY);
        }

        @Test
        @DisplayName("activate() 후 histories에 이력이 추가된다")
        void historyIsAdded() {
            subscription.activate(BILLING_KEY);

            assertThat(subscription.getHistories()).hasSize(1);
            SubscriptionHistory history = subscription.getHistories().get(0);
            assertThat(history.getFromStatus()).isEqualTo(SubscriptionStatus.PENDING_ACTIVATION);
            assertThat(history.getToStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("이미 ACTIVE 상태에서 activate() 재호출 시 예외 발생")
        void cannotActivateTwice() {
            subscription.activate(BILLING_KEY);
            assertThatThrownBy(() -> subscription.activate(BILLING_KEY))
                    .isInstanceOf(PaymentException.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @BeforeEach
        void activate() {
            subscription.activate(BILLING_KEY);
            subscription.pullDomainEvents();
        }

        @Test
        @DisplayName("ACTIVE → CANCELLING 전이 후 상태가 변경된다")
        void statusBecomesCancelling() {
            subscription.cancel("사용자 요청");
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLING);
        }

        @Test
        @DisplayName("cancel() 후 SubscriptionCancelledEvent가 발행된다")
        void emitsCancelledEvent() {
            subscription.cancel("환불 요청");

            List<Object> events = subscription.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(SubscriptionCancelledEvent.class);
        }

        @Test
        @DisplayName("CANCELLING → CANCELLED 전이 완료된다")
        void completeCancellation() {
            subscription.cancel("사용자 요청");
            subscription.pullDomainEvents();
            subscription.completeCancellation("기간 만료");

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("CANCELLING 상태에서 cancel() 재호출 시 예외 발생")
        void cannotCancelTwice() {
            subscription.cancel("사용자 요청");
            assertThatThrownBy(() -> subscription.cancel("중복 요청"))
                    .isInstanceOf(PaymentException.class);
        }
    }

    @Nested
    @DisplayName("suspend() / reactivate()")
    class SuspendReactivate {

        @BeforeEach
        void activate() {
            subscription.activate(BILLING_KEY);
            subscription.pullDomainEvents();
        }

        @Test
        @DisplayName("ACTIVE → SUSPENDED 전이 후 상태가 변경된다")
        void suspend() {
            subscription.suspend("결제 실패 3회");
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        @DisplayName("SUSPENDED → ACTIVE 전이(reactivate) 후 상태가 변경된다")
        void reactivate() {
            subscription.suspend("결제 실패");
            subscription.reactivate("카드 등록 완료");
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("SUSPENDED → CANCELLED 전이 후 상태가 변경된다")
        void suspendedToCancelled() {
            subscription.suspend("결제 실패");
            subscription.completeCancellation("장기 미납");
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("extendBillingDate()")
    class ExtendBillingDate {

        @Test
        @DisplayName("nextBillingDate가 새 날짜로 갱신된다")
        void dateIsUpdated() {
            LocalDate newDate = NEXT_BILLING.plusMonths(1);
            subscription.extendBillingDate(newDate);
            assertThat(subscription.getNextBillingDate()).isEqualTo(newDate);
        }
    }

    @Nested
    @DisplayName("pullDomainEvents()")
    class PullEvents {

        @Test
        @DisplayName("pullDomainEvents() 후 이벤트 목록은 비워진다")
        void pullClearsEvents() {
            subscription.activate(BILLING_KEY);
            subscription.pullDomainEvents();
            assertThat(subscription.pullDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getHistories()")
    class Histories {

        @Test
        @DisplayName("반환된 histories 목록은 불변이다")
        void historiesIsUnmodifiable() {
            List<SubscriptionHistory> histories = subscription.getHistories();
            assertThatThrownBy(() -> histories.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("저장소에서 복원된 Subscription은 입력 필드를 그대로 갖는다")
        void reconstitutePreservesFields() {
            UUID id = UUID.randomUUID();
            java.time.Instant now = java.time.Instant.now();
            Subscription restored = Subscription.reconstitute(
                    id, USER_ID, BILLING_KEY, SubscriptionStatus.ACTIVE,
                    NEXT_BILLING, now, now, 1L, List.of());

            assertThat(restored.getId()).isEqualTo(id);
            assertThat(restored.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(restored.getBillingKey()).isEqualTo(BILLING_KEY);
            assertThat(restored.getVersion()).isEqualTo(1L);
            assertThat(restored.pullDomainEvents()).isEmpty();
        }
    }
}
