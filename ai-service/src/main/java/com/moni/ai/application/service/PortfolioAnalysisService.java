package com.moni.ai.application.service;

import com.moni.ai.application.prompt.PortfolioAnalysisPromptBuilder;
import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.ai.infrastructure.client.LlmAnalysisRequest;
import com.moni.ai.infrastructure.client.LlmAnalysisResponse;
import com.moni.ai.infrastructure.client.LlmClient;
import com.moni.ai.presentation.dto.request.PortfolioAnalysisRequestDto;
import com.moni.ai.presentation.dto.response.PortfolioAnalysisResponseDto;
import com.moni.ai.presentation.dto.response.PortfolioTendencyAnalysisResponseDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioAnalysisService {

    private static final String MDC_ANALYSIS_ID = "analysisId";

    private final PortfolioAnalysisPromptBuilder promptBuilder;
    private final LlmClient llmClient;

    public PortfolioAnalysisResponseDto analyze(PortfolioAnalysisRequestDto request) {
        MDC.put(MDC_ANALYSIS_ID, request.analysisId().toString());
        try {
            LlmAnalysisRequest llmRequest = promptBuilder.build(request);
            LlmAnalysisResponse llmResponse = llmClient.analyzePortfolio(llmRequest);
            validateLlmResponse(request, llmResponse);

            return new PortfolioAnalysisResponseDto(
                    request.analysisId(),
                    llmResponse.summary(),
                    PortfolioTendencyAnalysisResponseDto.from(
                            request.tendencyAnalysis(),
                            llmResponse.tendencyAnalysis()
                    ),
                    llmResponse.recommendation()
            );
        } finally {
            MDC.remove(MDC_ANALYSIS_ID);
        }
    }

    private void validateLlmResponse(PortfolioAnalysisRequestDto request, LlmAnalysisResponse response) {
        if (response == null
                || response.summary() == null
                || response.summary().isBlank()
                || response.recommendation() == null
                || response.recommendation().isBlank()) {
            throw new CustomException(AiErrorCode.PORTFOLIO_LLM_RESPONSE_INVALID);
        }

        if (request.tendencyAnalysis() == null) {
            return;
        }

        if (response.tendencyAnalysis() == null
                || response.tendencyAnalysis().summary() == null
                || response.tendencyAnalysis().summary().isBlank()
                || response.tendencyAnalysis().recommendation() == null
                || response.tendencyAnalysis().recommendation().isBlank()) {
            throw new CustomException(AiErrorCode.PORTFOLIO_LLM_RESPONSE_INVALID);
        }
    }
}
