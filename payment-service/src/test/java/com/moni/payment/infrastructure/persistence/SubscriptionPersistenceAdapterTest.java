package com.moni.payment.infrastructure.persistence;

import com.moni.payment.domain.model.BillingKey;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionHistory;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@Import(SubscriptionPersistenceAdapter.class)
@DisplayName("SubscriptionPersistenceAdapter — @DataJpaTest (PostgreSQL)")
class SubscriptionPersistenceAdapterTest {

    static {
        System.setProperty("api.version", "1.41");
        System.setProperty("DOCKER_API_VERSION", "1.41");
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("subscription_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private SubscriptionPersistenceAdapter adapter;

    @Autowired
    private SubscriptionJpaRepository subscriptionJpaRepository;

    @Autowired
    private SubscriptionHistoryRepository subscriptionHistoryRepository;

    private Subscription createAndSavePendingSubscription(UUID userId) {
        Subscription subscription = Subscription.create(userId);
        return adapter.save(subscription);
    }

    private Subscription createAndSaveActiveSubscription(UUID userId, LocalDate nextBillingDate) {
        Subscription subscription = Subscription.create(userId);
        subscription.activate(BillingKey.of("bk-" + UUID.randomUUID()));
        subscription.extendBillingDate(nextBillingDate);
        return adapter.save(subscription);
    }

    @Nested
    @DisplayName("save() / findById()")
    class SaveAndFindById {

        @Test
        @DisplayName("저장한 Subscription을 ID로 조회할 수 있다")
        void saveAndFindById() {
            UUID userId = UUID.randomUUID();
            Subscription saved = createAndSavePendingSubscription(userId);

            Optional<Subscription> found = adapter.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 빈 Optional 반환")
        void findByIdReturnsEmptyWhenNotFound() {
            Optional<Subscription> found = adapter.findById(UUID.randomUUID());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveByUserId()")
    class FindActiveByUserId {

        @Test
        @DisplayName("ACTIVE 구독이 있으면 반환한다")
        void returnsActiveSubscription() {
            UUID userId = UUID.randomUUID();
            createAndSaveActiveSubscription(userId, LocalDate.now().plusMonths(1));

            Optional<Subscription> found = adapter.findActiveByUserId(userId);

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(found.get().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("PENDING_ACTIVATION 구독은 활성 구독으로 조회되지 않는다")
        void pendingSubscriptionIsNotActive() {
            UUID userId = UUID.randomUUID();
            createAndSavePendingSubscription(userId);

            Optional<Subscription> found = adapter.findActiveByUserId(userId);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("구독이 없으면 빈 Optional 반환")
        void returnsEmptyWhenNoSubscription() {
            Optional<Subscription> found = adapter.findActiveByUserId(UUID.randomUUID());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveSubscriptionsDueBefore()")
    class FindActiveSubscriptionsDueBefore {

        @Test
        @DisplayName("nextBillingDate가 기준일 이전인 ACTIVE 구독을 반환한다")
        void returnsSubscriptionsDueBefore() {
            UUID userId = UUID.randomUUID();
            createAndSaveActiveSubscription(userId, LocalDate.now().minusDays(1));

            List<Subscription> result = adapter.findActiveSubscriptionsDueBefore(LocalDate.now());

            assertThat(result).anyMatch(s -> s.getUserId().equals(userId));
        }

        @Test
        @DisplayName("nextBillingDate가 기준일 이후인 구독은 반환하지 않는다")
        void excludesSubscriptionsNotYetDue() {
            UUID userId = UUID.randomUUID();
            createAndSaveActiveSubscription(userId, LocalDate.now().plusMonths(1));

            List<Subscription> result = adapter.findActiveSubscriptionsDueBefore(LocalDate.now());

            assertThat(result).noneMatch(s -> s.getUserId().equals(userId));
        }
    }

    @Nested
    @DisplayName("saveHistory()")
    class SaveHistory {

        @Test
        @DisplayName("SubscriptionHistory를 저장할 수 있다")
        void saveHistory() {
            UUID userId = UUID.randomUUID();
            Subscription saved = createAndSaveActiveSubscription(userId, LocalDate.now().plusMonths(1));

            SubscriptionHistory history = SubscriptionHistory.of(
                    saved.getId(),
                    SubscriptionStatus.PENDING_ACTIVATION,
                    SubscriptionStatus.ACTIVE,
                    "최초 결제 성공");
            adapter.saveHistory(history);

            assertThat(subscriptionHistoryRepository.findAll())
                    .anyMatch(h -> h.getSubscriptionId().equals(saved.getId()));
        }
    }
}
