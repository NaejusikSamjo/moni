package com.moni.ai.presentation.controller;

import com.moni.ai.application.service.AiService;
import com.moni.ai.application.service.MarketNewsCollectService;
import com.moni.ai.application.service.NewsService;
import com.moni.ai.application.service.NewsCollectService;
import com.moni.ai.presentation.controller.docs.AiControllerDocs;
import com.moni.ai.presentation.dto.request.IssueAnalysisReqDto;
import com.moni.ai.presentation.dto.request.NewsCreateReqDto;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.ai.presentation.dto.response.NewsCreateResDto;
import com.moni.ai.presentation.dto.response.WatchCompanyResDto;
import com.moni.common.response.GlobalResponse;
import jakarta.validation.Valid;
import jakarta.ws.rs.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController implements AiControllerDocs {

    private final AiService aiService;
    private final NewsService newsService;
    private final MarketNewsCollectService marketNewsCollectService;

    @PostMapping("/{ticker}/issue-analysis")
    public ResponseEntity<GlobalResponse<CompanyIssueResDto>> createIssueAnalysis(
            @PathVariable("ticker") String ticker,
            @RequestBody(required = false) IssueAnalysisReqDto request
    ) {

        String question = request != null ? request.getQuestion() : null;
        CompanyIssueResDto result = aiService.analyze(ticker, question);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(201,result));
    }

    // 캐시된 분석 결과 조회 (LLM 호출 없음)
    @GetMapping("/{ticker}/issue-analysis")
    public ResponseEntity<GlobalResponse<CompanyIssueResDto>> getIssueAnalysis(
            @PathVariable("ticker") String ticker) {

        CompanyIssueResDto result = aiService.getLatestAnalysis(ticker);
        return ResponseEntity.ok(GlobalResponse.success(200,result));
    }

    @PostMapping("/news/fetch")
    public ResponseEntity<GlobalResponse<String>> createNewsVector(){
        newsService.collectAll();
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(201, "success"));
    }

    @GetMapping("")
    public ResponseEntity<GlobalResponse<List<WatchCompanyResDto>>> getWatchCompany(){
        return ResponseEntity.ok(GlobalResponse.success(200,newsService.getWatchList()));
    }

    @PostMapping("/news/ticker")
    public ResponseEntity<GlobalResponse<NewsCreateResDto>> createNews(
            @RequestBody @Valid NewsCreateReqDto newsCreateReqDto
            ){
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(201,newsService.createNews(newsCreateReqDto)));
    }

    @PostMapping("/news/market/fetch")
    public ResponseEntity<GlobalResponse<String>> fetchMarketNews() {
        marketNewsCollectService.collectMarketNews();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(201, "SUCCESS"));
    }

}
