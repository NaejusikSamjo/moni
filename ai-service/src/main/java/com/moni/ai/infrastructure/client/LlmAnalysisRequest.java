package com.moni.ai.infrastructure.client;

public record LlmAnalysisRequest(
        String systemPrompt,
        String userPrompt
) {
}
