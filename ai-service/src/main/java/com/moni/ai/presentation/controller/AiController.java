package com.moni.ai.presentation.controller;

import com.moni.ai.application.service.AiService;
import com.moni.ai.application.service.NewsService;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.ai.presentation.dto.response.MarketAnalysisResDto;
import com.moni.ai.presentation.dto.response.WatchCompanyResDto;
import com.moni.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController{

    private final AiService aiService;
    private final NewsService newsService;


    @PostMapping("/{ticker}/issue-analysis")
    @Operation(summary = "기업 이슈 분석 생성", description = "RAG 기반으로 기업 이슈를 분석합니다. 캐시가 있을 경우 400을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "분석 성공"),
            @ApiResponse(responseCode = "400", description = "이미 분석된 기업"),
            @ApiResponse(responseCode = "404", description = "등록되지 않은 ticker"),
            @ApiResponse(responseCode = "500", description = "LLM 호출 실패")
    })
    public ResponseEntity<GlobalResponse<CompanyIssueResDto>> createIssueAnalysis(
            @PathVariable("ticker") String ticker
    ) {
        CompanyIssueResDto result = aiService.companyAnalyze(ticker);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(201,result));
    }

    @PostMapping("/news-summary")
    @Operation(summary = "시장 뉴스 분석 생성", description = "키워드 기반 시장 뉴스를 분석합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "분석 성공"),
            @ApiResponse(responseCode = "404", description = "등록되지 않은 키워드"),
            @ApiResponse(responseCode = "500", description = "LLM 호출 실패")
    })
    public ResponseEntity<GlobalResponse<MarketAnalysisResDto>> createNewsAnalysis(
            @RequestParam("keyword") String keyword
    ){
        MarketAnalysisResDto result = aiService.analyzeMarket(keyword);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(201,result));
    }

    // 캐시된 분석 결과 조회 (LLM 호출 없음)
    @GetMapping("/{ticker}/issue-analysis")
    @Operation(summary = "기업 이슈 분석 조회", description = "캐시된 분석 결과를 조회합니다. LLM 호출 없이 저장된 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "분석 결과 없음 또는 등록되지 않은 ticker")
    })
    public ResponseEntity<GlobalResponse<CompanyIssueResDto>> getIssueAnalysis(
            @PathVariable("ticker") String ticker) {

        CompanyIssueResDto result = aiService.getLatestAnalysis(ticker);
        return ResponseEntity.ok(GlobalResponse.success(200,result));
    }


    @GetMapping("")
    @Operation(summary = "관심 기업 목록 조회", description = "뉴스 수집 대상 기업 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<GlobalResponse<List<WatchCompanyResDto>>> getWatchCompany(){
        return ResponseEntity.ok(GlobalResponse.success(200,newsService.getWatchList()));
    }



}
