package com.moni.ai.infrastructure.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.common.error.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient portfolioChatClient;
    private final ObjectMapper objectMapper;

    public SpringAiLlmClient(
            @Qualifier("portfolioChatClient") ChatClient portfolioChatClient,
            ObjectMapper objectMapper
    ) {
        this.portfolioChatClient = portfolioChatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmAnalysisResponse analyzePortfolio(LlmAnalysisRequest request) {
        try {
            String content = portfolioChatClient.prompt()
                    .system(request.systemPrompt())
                    .user(request.userPrompt())
                    .call()
                    .content();

            return parseResponse(content);
        } catch (CustomException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (isTimeout(exception)) {
                throw new CustomException(AiErrorCode.PORTFOLIO_LLM_TIMEOUT);
            }
            log.warn("포트폴리오 LLM 호출 실패: {}", exception.getMessage());
            throw new CustomException(AiErrorCode.PORTFOLIO_LLM_PROVIDER_FAILED);
        }
    }

    private LlmAnalysisResponse parseResponse(String content) {
        if (content == null || content.isBlank()) {
            throw new CustomException(AiErrorCode.PORTFOLIO_LLM_RESPONSE_INVALID);
        }

        String json = stripMarkdownFence(content);
        try {
            return objectMapper.readValue(json, LlmAnalysisResponse.class);
        } catch (JsonProcessingException exception) {
            throw new CustomException(AiErrorCode.PORTFOLIO_LLM_RESPONSE_INVALID);
        }
    }

    private String stripMarkdownFence(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        String withoutStartFence = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutStartFence.replaceFirst("\\s*```$", "").trim();
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getSimpleName().toLowerCase();
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (className.contains("timeout") || message.contains("timeout") || message.contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
