package com.moni.ai.application.service;

import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.ai.domain.repository.AiLogRepository;
import com.moni.ai.presentation.dto.response.AiNewsAnalysisResDto;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmAnalysisService {

    private final ChatClient chatClient;
    private final AiLogRepository aiLogRepository;

    @Value("classpath:prompts/ai-system-prompt.st")
    private Resource systemPromptResource;

    @Retryable(
            retryFor = {CustomException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public AiNewsAnalysisResDto createLlmAnalysis(String systemPrompt, String query, String filterExpression){
        BeanOutputConverter<AiNewsAnalysisResDto> parser = new BeanOutputConverter<>(AiNewsAnalysisResDto.class);

        log.info("RAG 분석 시작 - 질의: {}", query);
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .advisors(advisor -> advisor
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression))
                .call()
                .content();

        if (response == null) {
            throw new CustomException(AiErrorCode.AI_RESPONSE_FAILED);
        }

        try {
            return parser.convert(response);
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", response);
            throw new CustomException(AiErrorCode.AI_RESPONSE_FAILED);
        }
    }

    @Recover
    public AiNewsAnalysisResDto analyzeRecover(CustomException e, String ticker, String question) {
        log.error("[{}] AI 분석 최종 실패: {}", ticker, e.getMessage());
        throw new CustomException(AiErrorCode.AI_RESPONSE_FAILED);
    }


}
