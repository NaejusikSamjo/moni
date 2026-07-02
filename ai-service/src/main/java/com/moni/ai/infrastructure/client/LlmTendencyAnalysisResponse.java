package com.moni.ai.infrastructure.client;

public record LlmTendencyAnalysisResponse(
        String summary,
        String recommendation
) {
}
