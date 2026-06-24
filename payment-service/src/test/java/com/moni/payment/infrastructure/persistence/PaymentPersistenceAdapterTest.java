package com.moni.payment.infrastructure.persistence;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.infrastructure.repository.PaymentHistoryRepository;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@Import(PaymentPersistenceAdapter.class)
@DisplayName("PaymentPersistenceAdapter — @DataJpaTest (PostgreSQL)")
class PaymentPersistenceAdapterTest {

    static {
        System.setProperty("api.version", "1.41");
        System.setProperty("DOCKER_API_VERSION", "1.41");
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_test")
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
    private PaymentPersistenceAdapter adapter;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    private static final UUID USER_ID = UUID.randomUUID();

    private Payment createPendingPayment(String merchantSuffix) {
        return Payment.initiate(
                USER_ID,
                MerchantId.of("MONI" + merchantSuffix),
                Money.of(9900L),
                PaymentType.SUBSCRIPTION_INITIAL,
                Instant.now().plusSeconds(600),
                USER_ID.toString());
    }

    @Nested
    @DisplayName("save() / findById()")
    class SaveAndFindById {

        @Test
        @DisplayName("저장한 Payment를 ID로 조회할 수 있다")
        void saveAndFindById() {
            Payment payment = createPendingPayment("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            adapter.save(payment);

            Optional<Payment> found = adapter.findById(payment.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(payment.getId());
            assertThat(found.get().getUserId()).isEqualTo(USER_ID);
            assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 빈 Optional 반환")
        void findByIdReturnsEmptyWhenNotFound() {
            Optional<Payment> found = adapter.findById(UUID.randomUUID());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMerchantId()")
    class FindByMerchantId {

        @Test
        @DisplayName("저장한 Payment를 merchantId로 조회할 수 있다")
        void findByMerchantId() {
            Payment payment = createPendingPayment("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
            adapter.save(payment);

            Optional<Payment> found = adapter.findByMerchantId(payment.getMerchantId());

            assertThat(found).isPresent();
            assertThat(found.get().getMerchantId()).isEqualTo(payment.getMerchantId());
        }

        @Test
        @DisplayName("존재하지 않는 merchantId 조회 시 빈 Optional 반환")
        void findByMerchantIdReturnsEmptyWhenNotFound() {
            Optional<Payment> found = adapter.findByMerchantId(MerchantId.of("MONI" + "x".repeat(32)));
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId() 페이지네이션")
    class FindByUserIdPaged {

        @Test
        @DisplayName("userId로 결제 목록을 페이지네이션하여 조회한다")
        void findByUserIdPaged() {
            adapter.save(createPendingPayment("ccccccccccccccccccccccccccccccccc"));
            adapter.save(createPendingPayment("ddddddddddddddddddddddddddddddddd"));

            List<Payment> result = adapter.findByUserId(USER_ID, 0, 10);

            assertThat(result).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result).allMatch(p -> p.getUserId().equals(USER_ID));
        }

        @Test
        @DisplayName("page=1 조회 시 두 번째 페이지만 반환한다")
        void findByUserIdSecondPage() {
            UUID specificUserId = UUID.randomUUID();
            for (int i = 0; i < 3; i++) {
                Payment p = Payment.initiate(
                        specificUserId,
                        MerchantId.of("MONI" + "e" + i + "e".repeat(29)),
                        Money.of(9900L), PaymentType.SUBSCRIPTION_INITIAL,
                        Instant.now().plusSeconds(600), specificUserId.toString());
                adapter.save(p);
            }

            List<Payment> page1 = adapter.findByUserId(specificUserId, 0, 2);
            List<Payment> page2 = adapter.findByUserId(specificUserId, 1, 2);

            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(1);
        }
    }

    @Nested
    @DisplayName("saveHistory()")
    class SaveHistory {

        @Test
        @DisplayName("PaymentHistory를 저장할 수 있다")
        void saveHistory() {
            Payment payment = createPendingPayment("fffffffffffffffffffffffffffffffff");
            adapter.save(payment);

            PaymentHistory history = PaymentHistory.of(
                    payment.getId(),
                    PaymentStatus.PENDING,
                    PaymentStatus.COMPLETED,
                    "{\"status\":\"DONE\"}",
                    Instant.now(),
                    USER_ID.toString());

            adapter.saveHistory(history);

            assertThat(paymentHistoryRepository.findAll())
                    .anyMatch(h -> h.getPaymentId().equals(payment.getId()));
        }
    }
}
