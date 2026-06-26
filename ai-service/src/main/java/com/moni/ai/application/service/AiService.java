package com.moni.ai.application.service;

import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.enums.SentimentEnum;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.domain.repository.CompanyIssueAnalysisRepository;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient chatClient;
    private final CompanyIssueAnalysisRepository companyIssueAnalysisRepository;

    //분석 결과 유효 시간
    private static final int CACHE_HOURS = 6;

    @Transactional
    public CompanyIssueResDto analyze(String ticker, String question) {

        WatchCompany company = WatchCompany.fromTicker(ticker);

        // 1. 유효한 캐시 조회
        // TODO: queryDSL도입 예정
        Optional<CompanyIssueAnalysisEntity> cached = companyIssueAnalysisRepository
                .findTopByTickerAndExpiredAtAfterOrderByCreatedAtDesc(ticker, LocalDateTime.now());

        if (cached.isPresent()) {
            log.info("[{}] 캐시된 분석 결과 반환", ticker);
            return CompanyIssueResDto.toDto(cached.get());
        }


        // 3. 질의 생성 (null이면 ticker 기반 기본 질의)
        String query = (question != null && !question.isBlank())
                ? "[" + ticker + " " + company.getCompanyName() + "] " + question
                : "[" + ticker + " " + company.getCompanyName() + "] " + company.getCompanyName() + " 기업의 최근 주요 이슈와 뉴스만 분석해줘. 다른 기업 정보는 제외해줘.";

        // 4. RAG + LLM 호출
        log.info("[{}] RAG 분석 시작 - 질의: {}", ticker, query);
        String response = chatClient.prompt()
                .system(sp -> sp.param("ticker", ticker)
                        .param("companyName", company.getCompanyName()))
                .user(query)
                .advisors(advisor -> advisor
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION,
                                "ticker == '" + ticker + "'"))
                .call()
                .content();

        // 5. sentiment 추출 (응답에서 POSITIVE/NEGATIVE/NEUTRAL 파싱)
        SentimentEnum sentiment = extractSentiment(response);

        // 6. 분석 결과 저장
        CompanyIssueAnalysisEntity entity = CompanyIssueAnalysisEntity.builder()
                .ticker(ticker)
                .companyName(company.getCompanyName())
                .summary(response)
                .sentiment(sentiment)
                .expiredAt(LocalDateTime.now().plusHours(CACHE_HOURS))
                .build();

        companyIssueAnalysisRepository.save(entity);
        log.info("[{}] 분석 결과 저장 완료 - sentiment: {}", ticker, sentiment);

        return CompanyIssueResDto.toDto(entity);
    }

    private SentimentEnum extractSentiment(String response) {
        if (response.contains("POSITIVE")) return SentimentEnum.POSITIVE;
        if (response.contains("NEGATIVE")) return SentimentEnum.NEGATIVE;
        if (response.contains("NEUTRAL")) return SentimentEnum.NEUTRAL;
        return null;
    }


    public CompanyIssueResDto getLatestAnalysis(String ticker) {

        WatchCompany.fromTicker(ticker);

        return companyIssueAnalysisRepository
                .findTopByTickerAndExpiredAtAfterOrderByCreatedAtDesc(ticker, LocalDateTime.now())
                .map(CompanyIssueResDto::toDto)
                .orElseThrow(() -> new CustomException(AiErrorCode.AI_NOT_FOUND));
    }
}
