package com.moni.payment.domain.model;

import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.SubscriptionActivatedEvent;
import com.moni.payment.domain.event.SubscriptionCancelledEvent;
import com.moni.payment.domain.event.SubscriptionSuspendedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Subscription 도메인 테스트")
class SubscriptionTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final BillingKey BILLING_KEY = BillingKey.of("test-billing-key-001");
    private static final Money AMOUNT = Money.of(new BigDecimal("9900"));

    private Subscription pendingSubscription() {
        return Subscription.create(USER_ID);
    }

    private Subscription activeSubscription() {
        Subscription s = pendingSubscription();
        s.activate(BILLING_KEY, AMOUNT);
        return s;
    }

    @Nested
    @DisplayName("Subscription.create()")
    class Create {

        @Test
        void 생성_시_상태는_PENDING_ACTIVATION이다() {
            Subscription subscription = pendingSubscription();

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_ACTIVATION);
        }

        @Test
        void 생성_시_userId가_설정된다() {
            Subscription subscription = pendingSubscription();

            assertThat(subscription.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        void 생성_시_billingKey는_null이다() {
            Subscription subscription = pendingSubscription();

            assertThat(subscription.getBillingKey()).isNull();
        }

        @Test
        void 생성_시_retryCount는_0이다() {
            Subscription subscription = pendingSubscription();

            assertThat(subscription.getRetryCount()).isZero();
        }
    }

    @Nested
    @DisplayName("subscription.activate()")
    class Activate {

        @Test
        void PENDING_ACTIVATION에서_ACTIVE로_전이한다() {
            Subscription subscription = pendingSubscription();

            subscription.activate(BILLING_KEY, AMOUNT);

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void activate_후_billingKey와_amount가_설정된다() {
            Subscription subscription = pendingSubscription();

            subscription.activate(BILLING_KEY, AMOUNT);

            assertThat(subscription.getBillingKey()).isEqualTo(BILLING_KEY);
            assertThat(subscription.getAmount()).isEqualTo(AMOUNT);
        }

        @Test
        void activate_후_SubscriptionActivatedEvent가_발행된다() {
            Subscription subscription = pendingSubscription();

            subscription.activate(BILLING_KEY, AMOUNT);

            List<Object> events = subscription.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(SubscriptionActivatedEvent.class);
        }

        @Test
        void activate_후_구독_이력이_기록된다() {
            Subscription subscription = pendingSubscription();

            subscription.activate(BILLING_KEY, AMOUNT);

            assertThat(subscription.getHistories()).hasSize(1);
            assertThat(subscription.getHistories().get(0).getFromStatus())
                    .isEqualTo(SubscriptionStatus.PENDING_ACTIVATION);
            assertThat(subscription.getHistories().get(0).getToStatus())
                    .isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void ACTIVE_상태에서_activate_호출_시_SUB_001_예외가_발생한다() {
            Subscription subscription = activeSubscription();

            assertThatThrownBy(() -> subscription.activate(BILLING_KEY, AMOUNT))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("subscription.cancel()")
    class Cancel {

        @Test
        void ACTIVE에서_CANCELLING으로_전이한다() {
            Subscription subscription = activeSubscription();

            subscription.cancel("사용자 요청");

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLING);
        }

        @Test
        void cancel_후_billingKeyDeletedAt이_설정된다() {
            Subscription subscription = activeSubscription();

            subscription.cancel("사용자 요청");

            assertThat(subscription.getBillingKeyDeletedAt()).isNotNull();
        }

        @Test
        void cancel_후_nextBillingDate는_유지된다() {
            Subscription subscription = activeSubscription();
            LocalDate originalNextBillingDate = subscription.getNextBillingDate();

            subscription.cancel("사용자 요청");

            assertThat(subscription.getNextBillingDate()).isEqualTo(originalNextBillingDate);
        }

        @Test
        void cancel_후_도메인_이벤트가_발행되지_않는다() {
            Subscription subscription = activeSubscription();
            subscription.pullDomainEvents(); // activate 이벤트 소비

            subscription.cancel("사용자 요청");

            assertThat(subscription.pullDomainEvents()).isEmpty();
        }

        @Test
        void PENDING_ACTIVATION_상태에서_cancel_호출_시_SUB_001_예외가_발생한다() {
            Subscription subscription = pendingSubscription();

            assertThatThrownBy(() -> subscription.cancel("테스트"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("subscription.reactivateFromCancelling()")
    class ReactivateFromCancelling {

        @Test
        void CANCELLING에서_ACTIVE로_전이한다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");
            BillingKey newKey = BillingKey.of("new-billing-key-002");

            subscription.reactivateFromCancelling(newKey);

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void reactivateFromCancelling_후_billingKey가_갱신된다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");
            BillingKey newKey = BillingKey.of("new-billing-key-002");

            subscription.reactivateFromCancelling(newKey);

            assertThat(subscription.getBillingKey()).isEqualTo(newKey);
        }

        @Test
        void reactivateFromCancelling_후_billingKeyDeletedAt이_초기화된다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");

            subscription.reactivateFromCancelling(BillingKey.of("new-billing-key-002"));

            assertThat(subscription.getBillingKeyDeletedAt()).isNull();
        }

        @Test
        void reactivateFromCancelling_후_nextBillingDate가_한달_후로_설정된다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");

            subscription.reactivateFromCancelling(BillingKey.of("new-billing-key-002"));

            assertThat(subscription.getNextBillingDate())
                    .isAfterOrEqualTo(LocalDate.now().plusMonths(1).minusDays(1));
        }

        @Test
        void ACTIVE_상태에서_reactivateFromCancelling_호출_시_SUB_001_예외가_발생한다() {
            Subscription subscription = activeSubscription();

            assertThatThrownBy(() ->
                    subscription.reactivateFromCancelling(BillingKey.of("new-billing-key-002")))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("subscription.completeCancellation()")
    class CompleteCancellation {

        @Test
        void CANCELLING에서_CANCELLED로_전이한다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");

            subscription.completeCancellation("만료일 도래");

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        void completeCancellation_후_이력이_기록된다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");
            int prevSize = subscription.getHistories().size();

            subscription.completeCancellation("만료일 도래");

            assertThat(subscription.getHistories()).hasSize(prevSize + 1);
            assertThat(subscription.getHistories().get(prevSize).getToStatus())
                    .isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        void completeCancellation_후_SubscriptionCancelledEvent가_발행된다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");
            subscription.pullDomainEvents(); // cancel 이벤트 없지만 클리어

            subscription.completeCancellation("만료일 도래");

            List<Object> events = subscription.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(SubscriptionCancelledEvent.class);
        }

        @Test
        void ACTIVE_상태에서_completeCancellation_호출_시_SUB_001_예외가_발생한다() {
            Subscription subscription = activeSubscription();

            assertThatThrownBy(() -> subscription.completeCancellation("테스트"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION));
        }

        @Test
        void CANCELLED_상태는_단말_상태로_이후_전이가_불가하다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");
            subscription.completeCancellation("만료일 도래");

            assertThatThrownBy(() -> subscription.completeCancellation("재시도"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("subscription.suspend()")
    class Suspend {

        @Test
        void ACTIVE에서_SUSPENDED로_전이한다() {
            Subscription subscription = activeSubscription();

            subscription.suspend("결제 실패");

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        void suspend_후_이력이_기록된다() {
            Subscription subscription = activeSubscription();
            int prevSize = subscription.getHistories().size();

            subscription.suspend("결제 실패");

            assertThat(subscription.getHistories()).hasSize(prevSize + 1);
            assertThat(subscription.getHistories().get(prevSize).getToStatus())
                    .isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        void suspend_후_SubscriptionSuspendedEvent가_발행된다() {
            Subscription subscription = activeSubscription();
            subscription.pullDomainEvents(); // activate 이벤트 소비

            subscription.suspend("결제 실패");

            List<Object> events = subscription.pullDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(SubscriptionSuspendedEvent.class);
        }

        @Test
        void CANCELLING_상태에서_suspend_호출_시_SUB_001_예외가_발생한다() {
            Subscription subscription = activeSubscription();
            subscription.cancel("테스트");

            assertThatThrownBy(() -> subscription.suspend("결제 실패"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("subscription.reactivate()")
    class Reactivate {

        @Test
        void SUSPENDED에서_ACTIVE로_전이한다() {
            Subscription subscription = activeSubscription();
            subscription.suspend("결제 실패");

            subscription.reactivate("사용자 재활성화");

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void reactivate_후_이력이_기록된다() {
            Subscription subscription = activeSubscription();
            subscription.suspend("결제 실패");
            int prevSize = subscription.getHistories().size();

            subscription.reactivate("사용자 재활성화");

            assertThat(subscription.getHistories()).hasSize(prevSize + 1);
            assertThat(subscription.getHistories().get(prevSize).getToStatus())
                    .isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void ACTIVE_상태에서_reactivate_호출_시_SUB_001_예외가_발생한다() {
            Subscription subscription = activeSubscription();

            assertThatThrownBy(() -> subscription.reactivate("테스트"))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getErrorCode())
                            .isEqualTo(PaymentErrorCode.INVALID_SUBSCRIPTION_STATUS_TRANSITION));
        }
    }

    @Nested
    @DisplayName("subscription.incrementRetryCount() / resetRetryCount()")
    class RetryCount {

        @Test
        void incrementRetryCount_호출_시_retryCount가_1_증가한다() {
            Subscription subscription = activeSubscription();

            subscription.incrementRetryCount();

            assertThat(subscription.getRetryCount()).isEqualTo(1);
        }

        @Test
        void incrementRetryCount_3회_호출_시_retryCount는_3이다() {
            Subscription subscription = activeSubscription();

            subscription.incrementRetryCount();
            subscription.incrementRetryCount();
            subscription.incrementRetryCount();

            assertThat(subscription.getRetryCount()).isEqualTo(3);
        }

        @Test
        void resetRetryCount_호출_시_retryCount가_0으로_초기화된다() {
            Subscription subscription = activeSubscription();
            subscription.incrementRetryCount();
            subscription.incrementRetryCount();

            subscription.resetRetryCount();

            assertThat(subscription.getRetryCount()).isZero();
        }
    }

    @Nested
    @DisplayName("subscription.extendBillingDate()")
    class ExtendBillingDate {

        @Test
        void nextBillingDate가_지정한_날짜로_변경된다() {
            Subscription subscription = activeSubscription();
            LocalDate newDate = LocalDate.now().plusMonths(2);

            subscription.extendBillingDate(newDate);

            assertThat(subscription.getNextBillingDate()).isEqualTo(newDate);
        }
    }

    @Nested
    @DisplayName("subscription.pullDomainEvents()")
    class PullDomainEvents {

        @Test
        void pullDomainEvents_호출_후_이벤트_목록이_비워진다() {
            Subscription subscription = activeSubscription();

            subscription.pullDomainEvents();

            assertThat(subscription.pullDomainEvents()).isEmpty();
        }
    }
}
