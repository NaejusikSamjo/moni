package com.moni.portfolio.application.service;

import com.moni.portfolio.application.analysis.PortfolioRiskCalculator;
import com.moni.portfolio.application.analysis.TendencySuitabilityResult;
import com.moni.portfolio.application.analysis.TendencyType;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.infrastructure.client.AiServiceClient;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.infrastructure.client.UserServiceClient;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.AiPortfolioAnalysisResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.AiPortfolioTendencyAnalysisResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetAnalysisSnapshotResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.UserTendencyResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("PortfolioAnalysisAsyncExecutor 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioAnalysisAsyncExecutorTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID ANALYSIS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private TradeServiceClient tradeServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PortfolioRiskCalculator portfolioRiskCalculator;

    @Mock
    private PortfolioAnalysisRepository portfolioAnalysisRepository;

    @InjectMocks
    private PortfolioAnalysisAsyncExecutor portfolioAnalysisAsyncExecutor;

    @Nested
    @DisplayName("requestAiAnalysis()")
    class RequestAiAnalysis {

        @Test
        @DisplayName("성공 - trade/user 스냅샷을 기반으로 AI 응답을 분석 성공 상태로 저장한다")
        void success_update_success() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            UserTendencyResponseDto userTendency = new UserTendencyResponseDto(
                    UUID.fromString("00000000-0000-0000-0000-000000000010"),
                    33,
                    "STABLE"
            );
            AiPortfolioAnalysisResponseDto response = new AiPortfolioAnalysisResponseDto(
                    ANALYSIS_ID,
                    "요약 문장입니다.",
                    new AiPortfolioTendencyAnalysisResponseDto(
                            "STABLE",
                            "안정추구형",
                            33,
                            "ACTIVE",
                            "적극투자형",
                            70,
                            63,
                            "보통",
                            "사용자 성향보다 포트폴리오 위험도가 높은 편입니다.",
                            "단일 종목 비중을 낮추는 방향을 검토해볼 수 있습니다."
                    ),
                    "분산 투자를 검토해볼 수 있습니다."
            );

            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));
            givenPendingAnalysisExists();
            givenTradeSnapshot();
            given(userServiceClient.getTendency(USER_ID)).willReturn(success(userTendency));
            given(portfolioRiskCalculator.calculate(
                    any(UserTendencyResponseDto.class),
                    any(BigDecimal.class),
                    any(BigDecimal.class),
                    any(BigDecimal.class),
                    anyInt()
            )).willReturn(new TendencySuitabilityResult(
                    TendencyType.STABLE,
                    33,
                    TendencyType.ACTIVE,
                    70,
                    63,
                    "보통"
            ));
            given(aiServiceClient.analyzePortfolio(eq(USER_ID), eq("USER"), any(AiPortfolioAnalysisRequestDto.class)))
                    .willReturn(new ExternalApiResponseDto<>(200, "SUCCESS", response, null));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, USER_ID);

            // then
            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
            assertThat(analysis.getSummary())
                    .contains("요약 문장입니다.")
                    .contains("성향 적합도: 사용자 성향보다 포트폴리오 위험도가 높은 편입니다.")
                    .contains("성향 권고: 단일 종목 비중을 낮추는 방향을 검토해볼 수 있습니다.")
                    .contains("권고: 분산 투자를 검토해볼 수 있습니다.");
            assertThat(analysis.getTotalReturnRate()).isEqualByComparingTo("-1.7750");
            assertThat(analysis.getTotalEvaluationAmount()).isEqualByComparingTo("4158500.00");
            assertThat(analysis.getConcentrationScore()).isEqualByComparingTo("50.50");
            assertThat(analysis.getConcentrationThreshold()).isEqualByComparingTo("60.00");
            assertThat(analysis.getErrorMessage()).isNull();
            assertThat(analysis.getPortfolio().getAiAnalysisCount()).isEqualTo(1L);

            ArgumentCaptor<AiPortfolioAnalysisRequestDto> requestCaptor =
                    ArgumentCaptor.forClass(AiPortfolioAnalysisRequestDto.class);
            then(aiServiceClient).should().analyzePortfolio(eq(USER_ID), eq("USER"), requestCaptor.capture());
            AiPortfolioAnalysisRequestDto request = requestCaptor.getValue();
            assertThat(request.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(request.totalEvaluationAmount()).isEqualByComparingTo("4158500.00");
            assertThat(request.totalReturnRate()).isEqualByComparingTo("-1.7750");
            assertThat(request.concentrationScore()).isEqualByComparingTo("50.50");
            assertThat(request.tendencyAnalysis()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 투자 성향 조회 응답이 올바르지 않으면 성향 분석만 생략하고 기본 분석을 요청한다")
        void success_fallback_user_tendency_invalid_response() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            AiPortfolioAnalysisResponseDto response = new AiPortfolioAnalysisResponseDto(
                    ANALYSIS_ID,
                    "요약 문장입니다.",
                    null,
                    "권고 문장입니다."
            );

            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));
            givenPendingAnalysisExists();
            givenTradeSnapshot();
            given(userServiceClient.getTendency(USER_ID)).willReturn(success(null));
            given(aiServiceClient.analyzePortfolio(eq(USER_ID), eq("USER"), any(AiPortfolioAnalysisRequestDto.class)))
                    .willReturn(new ExternalApiResponseDto<>(200, "SUCCESS", response, null));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, USER_ID);

            // then
            ArgumentCaptor<AiPortfolioAnalysisRequestDto> requestCaptor =
                    ArgumentCaptor.forClass(AiPortfolioAnalysisRequestDto.class);
            then(aiServiceClient).should().analyzePortfolio(eq(USER_ID), eq("USER"), requestCaptor.capture());
            assertThat(requestCaptor.getValue().tendencyAnalysis()).isNull();
            then(portfolioRiskCalculator).should(never()).calculate(
                    any(UserTendencyResponseDto.class),
                    any(BigDecimal.class),
                    any(BigDecimal.class),
                    any(BigDecimal.class),
                    anyInt()
            );
        }

        @Test
        @DisplayName("실패 - AI 응답 분석 ID가 다르면 실패 상태로 저장한다")
        void fail_invalid_ai_response() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            AiPortfolioAnalysisResponseDto response = new AiPortfolioAnalysisResponseDto(
                    UUID.fromString("00000000-0000-0000-0000-000000000099"),
                    "요약 문장입니다.",
                    null,
                    "권고 문장입니다."
            );

            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));
            givenPendingAnalysisExists();
            givenTradeSnapshot();
            given(userServiceClient.getTendency(USER_ID)).willReturn(success(null));
            given(aiServiceClient.analyzePortfolio(eq(USER_ID), eq("USER"), any(AiPortfolioAnalysisRequestDto.class)))
                    .willReturn(new ExternalApiResponseDto<>(200, "SUCCESS", response, null));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, USER_ID);

            // then
            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(analysis.getErrorMessage()).isEqualTo("AI 서비스 응답 데이터가 올바르지 않습니다.");
            assertThat(analysis.getPortfolio().getAiAnalysisCount()).isZero();
        }

        @Test
        @DisplayName("실패 - 보유 종목이 없으면 FAILED 상태로 저장하고 AI 서비스를 호출하지 않는다")
        void fail_empty_holdings() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));
            givenPendingAnalysisExists();
            given(tradeServiceClient.getAnalysisSnapshot(USER_ID))
                    .willReturn(success(snapshot("10000000.00", "0.00", "0.00", List.of())));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, USER_ID);

            // then
            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(analysis.getErrorMessage()).isEqualTo("잘못된 포트폴리오 조회 요청입니다.");
            then(aiServiceClient).should(never())
                    .analyzePortfolio(any(UUID.class), any(String.class), any(AiPortfolioAnalysisRequestDto.class));
        }

        @Test
        @DisplayName("성공 - AI 응답 전 상태가 변경되면 결과 저장을 건너뛴다")
        void success_skip_when_status_changed_before_ai_response_saved() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            AiPortfolioAnalysisResponseDto response = new AiPortfolioAnalysisResponseDto(
                    ANALYSIS_ID,
                    "요약 문장입니다.",
                    null,
                    "권고 문장입니다."
            );

            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));
            given(portfolioAnalysisRepository.existsByIdAndStatus(ANALYSIS_ID, AnalysisStatus.PENDING))
                    .willReturn(false);
            givenTradeSnapshot();
            given(userServiceClient.getTendency(USER_ID)).willReturn(success(null));
            given(aiServiceClient.analyzePortfolio(eq(USER_ID), eq("USER"), any(AiPortfolioAnalysisRequestDto.class)))
                    .willReturn(new ExternalApiResponseDto<>(200, "SUCCESS", response, null));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, USER_ID);

            // then
            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.PENDING);
            assertThat(analysis.getPortfolio().getAiAnalysisCount()).isZero();
        }

        @Test
        @DisplayName("성공 - 이미 성공 처리된 분석은 외부 API를 호출하지 않고 분석 횟수를 중복 증가시키지 않는다")
        void success_already_success_no_duplicate_count() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            analysis.succeed(
                    "이전 요약입니다.",
                    new BigDecimal("-1.7750"),
                    new BigDecimal("4158500.00"),
                    new BigDecimal("64.20"),
                    new BigDecimal("60.00")
            );
            ReflectionTestUtils.setField(analysis.getPortfolio(), "aiAnalysisCount", 1L);

            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, USER_ID);

            // then
            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
            assertThat(analysis.getPortfolio().getAiAnalysisCount()).isEqualTo(1L);
            then(tradeServiceClient).should(never()).getAnalysisSnapshot(any(UUID.class));
            then(userServiceClient).should(never()).getTendency(any(UUID.class));
            then(aiServiceClient).should(never())
                    .analyzePortfolio(any(UUID.class), any(String.class), any(AiPortfolioAnalysisRequestDto.class));
        }
    }

    private void givenPendingAnalysisExists() {
        given(portfolioAnalysisRepository.existsByIdAndStatus(ANALYSIS_ID, AnalysisStatus.PENDING))
                .willReturn(true);
    }

    private void givenTradeSnapshot() {
        given(tradeServiceClient.getAnalysisSnapshot(USER_ID))
                .willReturn(success(snapshot(
                        "10000000.00",
                        "4158500.00",
                        "-1.7750",
                        List.of(
                                holding(
                                        "000660",
                                        "SK하이닉스",
                                        10L,
                                        "220000.00",
                                        "210000.00",
                                        "2100000.00",
                                        "-100000.00",
                                        "-4.5455",
                                        "50.50"
                                ),
                                holding(
                                        "005930",
                                        "삼성전자",
                                        30L,
                                        "70000.00",
                                        "68616.67",
                                        "2058500.00",
                                        "-41500.00",
                                        "-1.9762",
                                        "49.50"
                                )
                        )
                )));
    }

    private PortfolioAnalysis pendingAnalysis() {
        Portfolio portfolio = Portfolio.create(USER_ID);
        ReflectionTestUtils.setField(portfolio, "id", UUID.fromString("00000000-0000-0000-0000-000000000010"));
        PortfolioAnalysis analysis = PortfolioAnalysis.request(portfolio);
        ReflectionTestUtils.setField(analysis, "id", ANALYSIS_ID);
        return analysis;
    }

    private TradeAssetAnalysisSnapshotResponseDto snapshot(
            String totalAsset,
            String stockEvaluationAmount,
            String totalReturnRate,
            List<TradeAssetHoldingResponseDto> holdings
    ) {
        return new TradeAssetAnalysisSnapshotResponseDto(
                new BigDecimal(totalAsset),
                BigDecimal.ZERO,
                new BigDecimal(stockEvaluationAmount),
                new BigDecimal("10000000.00"),
                BigDecimal.ZERO,
                new BigDecimal(totalReturnRate),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                holdings
        );
    }

    private TradeAssetHoldingResponseDto holding(
            String ticker,
            String stockName,
            Long quantity,
            String averagePurchasePrice,
            String currentPrice,
            String evaluationAmount,
            String profitLoss,
            String profitRate,
            String weight
    ) {
        return new TradeAssetHoldingResponseDto(
                ticker,
                stockName,
                quantity,
                new BigDecimal(averagePurchasePrice),
                new BigDecimal(currentPrice),
                new BigDecimal(evaluationAmount),
                new BigDecimal(profitLoss),
                new BigDecimal(profitRate),
                new BigDecimal(weight)
        );
    }

    private <T> ExternalApiResponseDto<T> success(T data) {
        return new ExternalApiResponseDto<>(200, "SUCCESS", data, null);
    }
}
