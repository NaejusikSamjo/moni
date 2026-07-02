package com.moni.ai.infrastructure.client;

public interface LlmClient {

    LlmAnalysisResponse analyzePortfolio(LlmAnalysisRequest request);
}
