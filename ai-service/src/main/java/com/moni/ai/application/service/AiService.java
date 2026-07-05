package com.moni.ai.application.service;

import com.moni.ai.domain.entity.MarketNewsAnalysisEntity;
import com.moni.ai.domain.enums.MarketKeyword;
import com.moni.ai.domain.exception.AiErrorCode;
import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.enums.SentimentEnum;
import com.moni.ai.domain.enums.WatchCompany;
import com.moni.ai.presentation.dto.response.AiNewsAnalysisResDto;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.ai.presentation.dto.response.MarketAnalysisResDto;
import com.moni.common.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AnalysisSaveService analysisSaveService;
    private final LlmAnalysisService llmAnalysisService;

    @Value("classpath:prompts/ai-system-prompt.st")
    private Resource companyPromptResource;

    @Value("classpath:prompts/market-system-prompt.st")
    private Resource marketPromptResource;

    //분석 결과 유효 시간
    private static final int CACHE_HOURS = 6;

    public CompanyIssueResDto companyAnalyze(String ticker) {

        WatchCompany company = WatchCompany.fromTicker(ticker);

        // 1. 유효한 캐시 조회
        Optional<CompanyIssueAnalysisEntity> cached = analysisSaveService.getCachedAnalysis(ticker);

        if (cached.isPresent()) {
            log.info("[{}] 분석 존재", ticker);
            throw new CustomException(AiErrorCode.ANALYSIS_ALREADY_EXISTS);
        }
        BeanOutputConverter<AiNewsAnalysisResDto> parser = new BeanOutputConverter<>(AiNewsAnalysisResDto.class);

        // 프롬프트 로드 및 변수 치환
        String systemPrompt = loadPrompt(companyPromptResource)
                .replace("{ticker}", ticker)
                .replace("{companyName}", company.getCompanyName())
                .replace("{format}", parser.getFormat());

        String query = "[" + ticker + " " + company.getCompanyName() + "] " + company.getCompanyName() + " 기업의 최근 주요 이슈와 뉴스만 분석해줘.";


        String twoDaysAgo = LocalDate.now().minusDays(1).toString();
        String filterExpression = "ticker == '" + ticker + "' && published_at >= '" + twoDaysAgo + "'";


        AiNewsAnalysisResDto result = llmAnalysisService.createLlmAnalysis(systemPrompt,query,filterExpression);
        // 5. sentiment 추출 (응답에서 POSITIVE/NEGATIVE/NEUTRAL 파싱)
        SentimentEnum sentiment = SentimentEnum.valueOf(result.getSentiment());
        // 6. 분석 결과 저장
        CompanyIssueAnalysisEntity entity = CompanyIssueAnalysisEntity.builder()
                .ticker(ticker)
                .companyName(company.getCompanyName())
                .summary(result.getSummary())
                .sentiment(sentiment)
                .expiredAt(LocalDateTime.now().plusHours(CACHE_HOURS))
                .build();

        CompanyIssueAnalysisEntity saved = analysisSaveService.save(entity);

        analysisSaveService.saveLog(combinePrompt(systemPrompt,query,filterExpression),saved,null);

        log.info("[{}] 분석 결과 저장 완료 - sentiment: {}", ticker, sentiment);

        return CompanyIssueResDto.toDto(saved);
    }

    public MarketAnalysisResDto analyzeMarket(String keyword) {


        MarketKeyword marketKeyword = MarketKeyword.fromKeyword(keyword);

        // 유효한 캐시 조회
        Optional<MarketNewsAnalysisEntity> cached = analysisSaveService.getCachedMarketAnalysis(keyword);
        if (cached.isPresent()) {
            log.info("[{}] 마켓 분석 존재", keyword);
            throw new CustomException(AiErrorCode.ANALYSIS_ALREADY_EXISTS);
        }

        BeanOutputConverter<AiNewsAnalysisResDto> parser = new BeanOutputConverter<>(AiNewsAnalysisResDto.class);

        String systemPrompt = loadPrompt(marketPromptResource)
                .replace("{keyword}", marketKeyword.getKeyword())
                .replace("{format}", parser.getFormat());

        String query = marketKeyword.getKeyword() + " 관련 최근 시장 이슈 분석해줘.";
        String twoDaysAgo = LocalDate.now().minusDays(1).toString();
        String filterExpression = "category == 'MARKET' && keyword == '" + keyword + "' && published_at >= '" + twoDaysAgo + "'";

        AiNewsAnalysisResDto llmResult = llmAnalysisService.createLlmAnalysis(systemPrompt, query, filterExpression);

        MarketNewsAnalysisEntity entity = MarketNewsAnalysisEntity.builder()
                .summary(llmResult.getSummary())
                .expiredAt(LocalDateTime.now().plusHours(CACHE_HOURS))
                .build();

        MarketNewsAnalysisEntity saved = analysisSaveService.save(entity);
        analysisSaveService.saveLog(combinePrompt(systemPrompt,query,filterExpression),null,saved);

        log.info("[{}] 분석 결과 저장 완료", keyword);
        return MarketAnalysisResDto.from(saved);
    }




    public CompanyIssueResDto getLatestAnalysis(String ticker) {

        WatchCompany.fromTicker(ticker);

        return analysisSaveService.getLatestAnalysis(ticker)
                .map(CompanyIssueResDto::toDto)
                .orElseThrow(()-> new CustomException(AiErrorCode.AI_NOT_FOUND));
    }



    private String loadPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CustomException(AiErrorCode.AI_RESPONSE_FAILED);
        }
    }

    private String combinePrompt(String systemPrompt,String query, String filterExpression){
        String prompt = "=== SYSTEM ===\n" + systemPrompt +
                "\n=== QUERY ===\n" + query +
                "\n=== FILTER ===\n" + filterExpression;

        return prompt;
    }

}
