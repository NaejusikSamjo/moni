package com.moni.portfolio.application.service;

import com.moni.common.error.exception.CustomException;
import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.exception.PortfolioErrorCode;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import com.moni.portfolio.infrastructure.client.AiServiceClient;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.AiPortfolioAnalysisResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioAnalysisAsyncExecutor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String DEFAULT_USER_ROLE = "USER";

    private final AiServiceClient aiServiceClient;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;

    @Async
    @Transactional
    public void requestAiAnalysis(UUID analysisId, AiPortfolioAnalysisRequestDto request) {
        PortfolioAnalysis analysis = portfolioAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(PortfolioErrorCode.PORTFOLIO_ANALYSIS_NOT_FOUND));

        try {
            ExternalApiResponseDto<AiPortfolioAnalysisResponseDto> response = aiServiceClient.analyzePortfolio(
                    request.userId(),
                    DEFAULT_USER_ROLE,
                    request
            );
            AiPortfolioAnalysisResponseDto data = validateResponse(analysisId, response);
            analysis.succeed(
                    mergeSummary(data),
                    request.concentrationScore(),
                    request.concentrationThreshold()
            );
        } catch (RetryableException exception) {
            analysis.fail(truncate(PortfolioErrorCode.AI_SERVICE_TIMEOUT.getMessage()));
        } catch (FeignException exception) {
            analysis.fail(truncate(PortfolioErrorCode.AI_SERVICE_ERROR.getMessage()));
        } catch (CustomException exception) {
            analysis.fail(truncate(exception.getMessage()));
        } catch (RuntimeException exception) {
            log.warn("포트폴리오 AI 분석 처리 실패. analysisId={}", analysisId, exception);
            analysis.fail(truncate(PortfolioErrorCode.AI_SERVICE_ERROR.getMessage()));
        }
    }

    private AiPortfolioAnalysisResponseDto validateResponse(
            UUID analysisId,
            ExternalApiResponseDto<AiPortfolioAnalysisResponseDto> response
    ) {
        if (response == null
                || response.data() == null
                || response.data().analysisId() == null
                || !analysisId.equals(response.data().analysisId())
                || response.data().summary() == null
                || response.data().summary().isBlank()
                || response.data().recommendation() == null
                || response.data().recommendation().isBlank()) {
            throw new CustomException(PortfolioErrorCode.AI_RESPONSE_INVALID);
        }
        if (response.data().tendencyAnalysis() != null
                && (response.data().tendencyAnalysis().summary() == null
                || response.data().tendencyAnalysis().summary().isBlank()
                || response.data().tendencyAnalysis().recommendation() == null
                || response.data().tendencyAnalysis().recommendation().isBlank())) {
            throw new CustomException(PortfolioErrorCode.AI_RESPONSE_INVALID);
        }

        return response.data();
    }

    private String mergeSummary(AiPortfolioAnalysisResponseDto data) {
        StringBuilder summary = new StringBuilder();
        summary.append(data.summary());
        if (data.tendencyAnalysis() != null) {
            summary.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("성향 적합도: ")
                    .append(data.tendencyAnalysis().summary())
                    .append(System.lineSeparator())
                    .append("성향 권고: ")
                    .append(data.tendencyAnalysis().recommendation());
        }
        summary.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("권고: ")
                .append(data.recommendation());

        return summary.toString();
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
