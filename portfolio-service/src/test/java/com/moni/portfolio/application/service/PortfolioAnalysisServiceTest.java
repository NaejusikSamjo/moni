package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.application.analysis.PortfolioRiskCalculator;
import com.moni.portfolio.application.analysis.TendencySuitabilityResult;
import com.moni.portfolio.application.analysis.TendencyType;
import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import com.moni.portfolio.infrastructure.client.TradeServiceClient;
import com.moni.portfolio.infrastructure.client.UserServiceClient;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioTendencyAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetHoldingsResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.TradeAssetResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.UserTendencyResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisCreateResponseDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
    private PortfolioRiskCalculator portfolioRiskCalculator;

    @Mock
    private TradeServiceClient tradeServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PortfolioAnalysisAsyncExecutor portfolioAnalysisAsyncExecutor;

    @InjectMocks
    private PortfolioAnalysisService portfolioAnalysisService;

    @Nested
    @DisplayName("requestAnalysis()")
    class RequestAnalysis {

        @Test
        @DisplayName("성공 - 외부 서비스 응답으로 분석 스냅샷을 만들고 AI 분석을 요청한다")
        void success_request_analysis() {
            // given
            Portfolio portfolio = portfolio();
            UserTendencyResponseDto userTendency = new UserTendencyResponseDto(
                    UUID.fromString("00000000-0000-0000-0000-000000000010"),
                    33,
                    "STABLE"
            );

            given(portfolioRepository.findByUserId(USER_ID)).willReturn(Optional.of(portfolio));
            given(tradeServiceClient.getAssets(USER_ID))
                    .willReturn(success(new TradeAssetResponseDto(
                            new BigDecimal("10000000.00"),
                            new BigDecimal("5841500.00"),
                            new BigDecimal("4158500.00"),
                            new BigDecimal("10000000.00"),
                            new BigDecimal("-177500.00"),
                            new BigDecimal("-1.7750")
                    )));
            given(tradeServiceClient.getAssetHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .willReturn(success(new TradeAssetHoldingsResponseDto(
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
                            ),
                            0,
                            10,
                            2,
                            1,
                            "evaluationAmount,desc"
                    )));
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
            assertThat(portfolio.getAiAnalysisCount()).isEqualTo(1L);

            ArgumentCaptor<AiPortfolioAnalysisRequestDto> aiRequestCaptor =
                    ArgumentCaptor.forClass(AiPortfolioAnalysisRequestDto.class);
            then(portfolioAnalysisAsyncExecutor).should().requestAiAnalysis(
                    any(UUID.class),
                    aiRequestCaptor.capture()
            );
            AiPortfolioAnalysisRequestDto aiRequest = aiRequestCaptor.getValue();
            assertThat(aiRequest.analysisId()).isEqualTo(ANALYSIS_ID);
            assertThat(aiRequest.userId()).isEqualTo(USER_ID);
            assertThat(aiRequest.totalEvaluationAmount()).isEqualByComparingTo("4158500.00");
            assertThat(aiRequest.totalReturnRate()).isEqualByComparingTo("-1.7750");
            assertThat(aiRequest.concentrationScore()).isEqualByComparingTo("50.50");
            assertThat(aiRequest.concentrationThreshold()).isEqualByComparingTo("60.00");
            assertThat(aiRequest.holdings()).hasSize(2);
            assertThat(aiRequest.holdings().getFirst().stockName()).isEqualTo("SK하이닉스");

            AiPortfolioTendencyAnalysisRequestDto tendencyAnalysis = aiRequest.tendencyAnalysis();
            assertThat(tendencyAnalysis).isNotNull();
            assertThat(tendencyAnalysis.userTendencyType()).isEqualTo("STABLE");
            assertThat(tendencyAnalysis.userTendencyLabel()).isEqualTo("안정추구형");
            assertThat(tendencyAnalysis.portfolioRiskType()).isEqualTo("ACTIVE");
            assertThat(tendencyAnalysis.suitabilityScore()).isEqualTo(63);
        }

        @Test
        @DisplayName("실패 - 보유 종목이 없으면 분석 요청을 저장하지 않는다")
        void fail_empty_holdings() {
            // given
            Portfolio portfolio = portfolio();
            given(portfolioRepository.findByUserId(USER_ID)).willReturn(Optional.of(portfolio));
            given(tradeServiceClient.getAssets(USER_ID))
                    .willReturn(success(new TradeAssetResponseDto(
                            new BigDecimal("10000000.00"),
                            new BigDecimal("10000000.00"),
                            BigDecimal.ZERO,
                            new BigDecimal("10000000.00"),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    )));
            given(tradeServiceClient.getAssetHoldings(USER_ID, 0, 10, "evaluationAmount,desc"))
                    .willReturn(success(new TradeAssetHoldingsResponseDto(
                            List.of(),
                            0,
                            10,
                            0,
                            0,
                            "evaluationAmount,desc"
                    )));

            // when & then
            assertThatThrownBy(() -> portfolioAnalysisService.requestAnalysis(USER_ID))
                    .isInstanceOfSatisfying(CustomException.class, exception ->
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(PortfolioErrorCode.INVALID_PORTFOLIO_QUERY));

            then(portfolioAnalysisRepository).should(never()).save(any(PortfolioAnalysis.class));
            then(portfolioAnalysisAsyncExecutor).should(never())
                    .requestAiAnalysis(any(UUID.class), any(AiPortfolioAnalysisRequestDto.class));
        }
    }

    private Portfolio portfolio() {
        Portfolio portfolio = Portfolio.create(USER_ID);
        ReflectionTestUtils.setField(portfolio, "id", PORTFOLIO_ID);
        return portfolio;
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
