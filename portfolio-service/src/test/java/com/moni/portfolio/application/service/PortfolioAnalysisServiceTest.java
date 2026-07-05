package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.common.response.paging.PageRes;
import com.moni.portfolio.application.policy.PortfolioAnalysisPolicyService;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisCreateResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@DisplayName("PortfolioAnalysisService 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioAnalysisServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID PORTFOLIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANALYSIS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioAnalysisRepository portfolioAnalysisRepository;

    @Mock
    private PortfolioAnalysisPolicyService portfolioAnalysisPolicyService;

    @Mock
    private PortfolioAnalysisAsyncExecutor portfolioAnalysisAsyncExecutor;

    @InjectMocks
    private PortfolioAnalysisService portfolioAnalysisService;

    @Nested
    @DisplayName("requestAnalysis()")
    class RequestAnalysis {

        @Test
        @DisplayName("성공 - 정책 검증 후 PENDING 분석을 저장하고 비동기 분석을 요청한다")
        void success_request_analysis() {
            // given
            Portfolio portfolio = portfolio();
            given(portfolioRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(portfolio));
            given(portfolioAnalysisRepository.save(any(PortfolioAnalysis.class)))
                    .willAnswer(invocation -> {
                        PortfolioAnalysis analysis = invocation.getArgument(0);
                        ReflectionTestUtils.setField(analysis, "id", ANALYSIS_ID);
                        return analysis;
                    });

            // when
            PortfolioAnalysisCreateResponseDto result = portfolioAnalysisService.requestAnalysis(USER_ID);

            // then
            assertThat(result.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(result.status()).isEqualTo(AnalysisStatus.PENDING);
            assertThat(portfolio.getAiAnalysisCount()).isZero();
            then(portfolioAnalysisPolicyService).should().validateRequest(USER_ID, portfolio);
            then(portfolioAnalysisAsyncExecutor).should().requestAiAnalysis(ANALYSIS_ID, USER_ID);
        }

        @Test
        @DisplayName("성공 - 오늘 진행 중인 분석이 있으면 기존 분석 ID를 반환한다")
        void success_return_existing_pending_analysis() {
            // given
            Portfolio portfolio = portfolio();
            PortfolioAnalysis pendingAnalysis = PortfolioAnalysis.request(portfolio);
            ReflectionTestUtils.setField(pendingAnalysis, "id", ANALYSIS_ID);

            given(portfolioRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(portfolio));
            given(portfolioAnalysisRepository.findPendingByPortfolioIdAndCreatedAtBetween(
                    eq(PORTFOLIO_ID),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).willReturn(Optional.of(pendingAnalysis));

            // when
            PortfolioAnalysisCreateResponseDto result = portfolioAnalysisService.requestAnalysis(USER_ID);

            // then
            assertThat(result.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(result.status()).isEqualTo(AnalysisStatus.PENDING);
            assertThat(portfolio.getAiAnalysisCount()).isZero();
            then(portfolioAnalysisPolicyService).should(never()).validateRequest(USER_ID, portfolio);
            then(portfolioAnalysisRepository).should(never()).save(any(PortfolioAnalysis.class));
            then(portfolioAnalysisAsyncExecutor).should(never()).requestAiAnalysis(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("실패 - 오늘 이미 분석을 요청했으면 추가 요청할 수 없다")
        void fail_daily_limit_exceeded() {
            // given
            Portfolio portfolio = portfolio();
            given(portfolioRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(portfolio));
            willThrow(new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_DAILY_LIMIT_EXCEEDED))
                    .given(portfolioAnalysisPolicyService)
                    .validateRequest(USER_ID, portfolio);

            // when & then
            assertThatThrownBy(() -> portfolioAnalysisService.requestAnalysis(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PORTFOLIO_ANALYSIS_DAILY_LIMIT_EXCEEDED));

            then(portfolioAnalysisRepository).should(never()).save(any(PortfolioAnalysis.class));
            then(portfolioAnalysisAsyncExecutor).should(never()).requestAiAnalysis(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("실패 - 무료 플랜은 계정당 최대 5번까지만 분석을 요청할 수 있다")
        void fail_free_plan_limit_exceeded() {
            // given
            Portfolio portfolio = portfolio();
            ReflectionTestUtils.setField(portfolio, "aiAnalysisCount", 5L);
            given(portfolioRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(portfolio));
            willThrow(new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_FREE_LIMIT_EXCEEDED))
                    .given(portfolioAnalysisPolicyService)
                    .validateRequest(USER_ID, portfolio);

            // when & then
            assertThatThrownBy(() -> portfolioAnalysisService.requestAnalysis(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.PORTFOLIO_ANALYSIS_FREE_LIMIT_EXCEEDED));

            then(portfolioAnalysisRepository).should(never()).save(any(PortfolioAnalysis.class));
            then(portfolioAnalysisAsyncExecutor).should(never()).requestAiAnalysis(any(UUID.class), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("getAnalyses()")
    class GetAnalyses {

        @Test
        @DisplayName("성공 - 분석 이력 목록은 FAILED를 제외하고 PENDING, SUCCESS만 조회한다")
        void success_exclude_failed_analysis() {
            // given
            Portfolio portfolio = portfolio();
            PortfolioAnalysis pendingAnalysis = PortfolioAnalysis.request(portfolio);
            ReflectionTestUtils.setField(pendingAnalysis, "id", ANALYSIS_ID);
            PortfolioAnalysis successAnalysis = PortfolioAnalysis.request(portfolio);
            successAnalysis.succeed(
                    "요약입니다.",
                    new BigDecimal("2.1200"),
                    new BigDecimal("5100000.00"),
                    new BigDecimal("45.00"),
                    new BigDecimal("60.00")
            );
            ReflectionTestUtils.setField(
                    successAnalysis,
                    "id",
                    UUID.fromString("00000000-0000-0000-0000-000000000003")
            );

            given(portfolioRepository.findByUserId(USER_ID)).willReturn(Optional.of(portfolio));
            given(portfolioAnalysisRepository.findAllByPortfolioIdAndStatusIn(
                    eq(PORTFOLIO_ID),
                    eq(List.of(AnalysisStatus.PENDING, AnalysisStatus.SUCCESS)),
                    any(PageRequest.class)
            )).willReturn(new PageImpl<>(
                    List.of(pendingAnalysis, successAnalysis),
                    PageRequest.of(0, 10),
                    2
            ));

            // when
            PageRes<PortfolioAnalysisResponseDto> result = portfolioAnalysisService.getAnalyses(USER_ID, 0, 10);

            // then
            assertThat(result.getContent())
                    .extracting(PortfolioAnalysisResponseDto::status)
                    .containsExactly(AnalysisStatus.PENDING, AnalysisStatus.SUCCESS);
            then(portfolioAnalysisRepository).should().findAllByPortfolioIdAndStatusIn(
                    eq(PORTFOLIO_ID),
                    eq(List.of(AnalysisStatus.PENDING, AnalysisStatus.SUCCESS)),
                    any(PageRequest.class)
            );
        }
    }

    private Portfolio portfolio() {
        Portfolio portfolio = Portfolio.create(USER_ID);
        ReflectionTestUtils.setField(portfolio, "id", PORTFOLIO_ID);
        return portfolio;
    }
}
