package com.moni.ai.infrastructure.client;

public record LlmAnalysisResponse(
        String summary,
        String recommendation
) {
}
