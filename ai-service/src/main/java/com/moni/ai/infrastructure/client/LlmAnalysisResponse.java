package com.moni.ai.infrastructure.client;

public record LlmAnalysisResponse(
        String summary,
        LlmTendencyAnalysisResponse tendencyAnalysis,
        String recommendation
) {
}
