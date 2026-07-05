package com.moni.portfolio.application.policy;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.enums.SubscriptionStatus;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.infrastructure.client.PaymentServiceClient;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.SubscriptionStatusResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("PortfolioAnalysisPolicyService 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioAnalysisPolicyServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID PORTFOLIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private PortfolioAnalysisRepository portfolioAnalysisRepository;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @InjectMocks
    private PortfolioAnalysisPolicyService portfolioAnalysisPolicyService;

    @Nested
    @DisplayName("validateRequest()")
    class ValidateRequest {

        @Test
        @DisplayName("성공 - 무료 플랜 사용자가 누적 5회 미만이고 오늘 분석 이력이 없으면 통과한다")
        void success_free_plan_under_limit() {
            // given
            Portfolio portfolio = portfolio(4L);
            givenNoAnalysisToday();
            given(paymentServiceClient.getSubscriptionStatus(USER_ID))
                    .willReturn(success(freePlan()));

            // when & then
            assertThatCode(() -> portfolioAnalysisPolicyService.validateRequest(USER_ID, portfolio))
                    .doesNotThrowAnyException();

            then(portfolioAnalysisRepository).should().existsByPortfolioIdAndStatusInAndCreatedAtBetween(
                    eq(PORTFOLIO_ID),
                    eq(List.of(AnalysisStatus.PENDING, AnalysisStatus.SUCCESS)),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("실패 - 오늘 이미 분석 이력이 있으면 결제 상태 조회 없이 차단한다")
        void fail_daily_limit_exceeded() {
            // given
            Portfolio portfolio = portfolio(0L);
            given(portfolioAnalysisRepository.existsByPortfolioIdAndStatusInAndCreatedAtBetween(
                    any(UUID.class),
                    anyList(),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).willReturn(true);

            // when & then
            assertThatThrownBy(() -> portfolioAnalysisPolicyService.validateRequest(USER_ID, portfolio))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PORTFOLIO_ANALYSIS_DAILY_LIMIT_EXCEEDED));

            then(paymentServiceClient).should(never()).getSubscriptionStatus(USER_ID);
        }

        @Test
        @DisplayName("실패 - 무료 플랜 사용자가 누적 5회 이상이면 차단한다")
        void fail_free_plan_limit_exceeded() {
            // given
            Portfolio portfolio = portfolio(5L);
            givenNoAnalysisToday();
            given(paymentServiceClient.getSubscriptionStatus(USER_ID))
                    .willReturn(success(freePlan()));

            // when & then
            assertThatThrownBy(() -> portfolioAnalysisPolicyService.validateRequest(USER_ID, portfolio))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PORTFOLIO_ANALYSIS_FREE_LIMIT_EXCEEDED));
        }

        @Test
        @DisplayName("성공 - 유료 플랜 사용자는 누적 5회 이상이어도 오늘 분석 이력이 없으면 통과한다")
        void success_paid_plan_over_free_limit() {
            // given
            Portfolio portfolio = portfolio(5L);
            givenNoAnalysisToday();
            given(paymentServiceClient.getSubscriptionStatus(USER_ID))
                    .willReturn(success(paidPlan()));

            // when & then
            assertThatCode(() -> portfolioAnalysisPolicyService.validateRequest(USER_ID, portfolio))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("실패 - Payment 서비스 구독 상태 응답 데이터가 없으면 차단한다")
        void fail_payment_response_invalid() {
            // given
            Portfolio portfolio = portfolio(0L);
            givenNoAnalysisToday();
            given(paymentServiceClient.getSubscriptionStatus(USER_ID))
                    .willReturn(success(null));

            // when & then
            assertThatThrownBy(() -> portfolioAnalysisPolicyService.validateRequest(USER_ID, portfolio))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PAYMENT_RESPONSE_INVALID));
        }
    }

    private void givenNoAnalysisToday() {
        given(portfolioAnalysisRepository.existsByPortfolioIdAndStatusInAndCreatedAtBetween(
                any(UUID.class),
                anyList(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(false);
    }

    private Portfolio portfolio(Long aiAnalysisCount) {
        Portfolio portfolio = Portfolio.create(USER_ID);
        ReflectionTestUtils.setField(portfolio, "id", PORTFOLIO_ID);
        ReflectionTestUtils.setField(portfolio, "aiAnalysisCount", aiAnalysisCount);
        return portfolio;
    }

    private SubscriptionStatusResponseDto freePlan() {
        return new SubscriptionStatusResponseDto(false, null, null, null, null);
    }

    private SubscriptionStatusResponseDto paidPlan() {
        return new SubscriptionStatusResponseDto(
                true,
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                SubscriptionStatus.ACTIVE,
                null,
                9900L
        );
    }

    private <T> ExternalApiResponseDto<T> success(T data) {
        return new ExternalApiResponseDto<>(200, "SUCCESS", data, null);
    }
}
