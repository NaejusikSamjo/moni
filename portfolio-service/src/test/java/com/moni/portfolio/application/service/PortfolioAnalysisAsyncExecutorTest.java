package com.moni.portfolio.application.service;

import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.infrastructure.client.AiServiceClient;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.AiPortfolioAnalysisResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.AiPortfolioTendencyAnalysisResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@DisplayName("PortfolioAnalysisAsyncExecutor 테스트")
@ExtendWith(MockitoExtension.class)
class PortfolioAnalysisAsyncExecutorTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID ANALYSIS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private PortfolioAnalysisRepository portfolioAnalysisRepository;

    @InjectMocks
    private PortfolioAnalysisAsyncExecutor portfolioAnalysisAsyncExecutor;

    @Nested
    @DisplayName("requestAiAnalysis()")
    class RequestAiAnalysis {

        @Test
        @DisplayName("성공 - AI 응답을 분석 성공 상태로 저장한다")
        void success_update_success() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            AiPortfolioAnalysisRequestDto request = request();
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
                            "섹터 집중도를 낮추는 방향을 검토해볼 수 있습니다."
                    ),
                    "분산 투자를 검토해볼 수 있습니다."
            );

            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));
            given(aiServiceClient.analyzePortfolio(USER_ID, "USER", request))
                    .willReturn(new ExternalApiResponseDto<>(200, "SUCCESS", response, null));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, request);

            // then
            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
            assertThat(analysis.getSummary())
                    .contains("요약 문장입니다.")
                    .contains("성향 적합도: 사용자 성향보다 포트폴리오 위험도가 높은 편입니다.")
                    .contains("성향 권고: 섹터 집중도를 낮추는 방향을 검토해볼 수 있습니다.")
                    .contains("권고: 분산 투자를 검토해볼 수 있습니다.");
            assertThat(analysis.getConcentrationScore()).isEqualByComparingTo("64.20");
            assertThat(analysis.getConcentrationThreshold()).isEqualByComparingTo("60.00");
            assertThat(analysis.getErrorMessage()).isNull();
            then(aiServiceClient).should().analyzePortfolio(USER_ID, "USER", request);
        }

        @Test
        @DisplayName("실패 - AI 응답 분석 ID가 다르면 실패 상태로 저장한다")
        void fail_invalid_ai_response() {
            // given
            PortfolioAnalysis analysis = pendingAnalysis();
            AiPortfolioAnalysisRequestDto request = request();
            AiPortfolioAnalysisResponseDto response = new AiPortfolioAnalysisResponseDto(
                    UUID.fromString("00000000-0000-0000-0000-000000000099"),
                    "요약 문장입니다.",
                    null,
                    "권고 문장입니다."
            );

            given(portfolioAnalysisRepository.findById(ANALYSIS_ID)).willReturn(Optional.of(analysis));
            given(aiServiceClient.analyzePortfolio(USER_ID, "USER", request))
                    .willReturn(new ExternalApiResponseDto<>(200, "SUCCESS", response, null));

            // when
            portfolioAnalysisAsyncExecutor.requestAiAnalysis(ANALYSIS_ID, request);

            // then
            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(analysis.getErrorMessage()).isEqualTo("AI 서비스 응답 데이터가 올바르지 않습니다.");
        }
    }

    private PortfolioAnalysis pendingAnalysis() {
        Portfolio portfolio = Portfolio.create(USER_ID);
        ReflectionTestUtils.setField(portfolio, "id", UUID.fromString("00000000-0000-0000-0000-000000000010"));
        PortfolioAnalysis analysis = PortfolioAnalysis.request(
                portfolio,
                new BigDecimal("-1.7750"),
                new BigDecimal("4158500.00")
        );
        ReflectionTestUtils.setField(analysis, "id", ANALYSIS_ID);
        return analysis;
    }

    private AiPortfolioAnalysisRequestDto request() {
        return new AiPortfolioAnalysisRequestDto(
                ANALYSIS_ID,
                USER_ID,
                new BigDecimal("4158500.00"),
                new BigDecimal("-1.7750"),
                new BigDecimal("64.20"),
                new BigDecimal("60.00"),
                List.of(),
                List.of(),
                null
        );
    }
}
