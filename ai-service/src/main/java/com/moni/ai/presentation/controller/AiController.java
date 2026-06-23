package com.moni.ai.presentation.controller;

import com.moni.ai.application.service.AiService;
import com.moni.ai.application.service.NewsCollectService;
import com.moni.ai.presentation.dto.request.IssueAnalysisReqDto;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.common.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private AiService aiService;
    private NewsCollectService newsCollectService;

    @PostMapping("/{ticker}/issue-analysis")
    public ResponseEntity<GlobalResponse<CompanyIssueResDto>> createIssueAnalysis(
            @PathVariable("ticker") String ticker,
            @RequestBody(required = false) IssueAnalysisReqDto request
    ) {

        String question = request != null ? request.getQuestion() : null;
        CompanyIssueResDto result = aiService.analyze(ticker, question);
        return ResponseEntity.ok(GlobalResponse.success(201,result));
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
        newsCollectService.collectAll();
        return ResponseEntity.ok(GlobalResponse.success(201, "success"));
    }
}
