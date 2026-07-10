package com.moni.ai.presentation.controller;

import com.moni.ai.application.service.MarketNewsCollectService;
import com.moni.ai.application.service.NewsService;
import com.moni.ai.presentation.dto.request.NewsCreateReqDto;
import com.moni.ai.presentation.dto.response.NewsCreateResDto;
import com.moni.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class NewsAdminController {

    private final NewsService newsService;
    private final MarketNewsCollectService marketNewsCollectService;

    @PostMapping("/news/fetch")
    @Operation(summary = "기업 뉴스 수동 수집", description = "등록된 전체 기업의 뉴스를 수동으로 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "수집 성공"),
            @ApiResponse(responseCode = "500", description = "수집 실패")
    })
    public ResponseEntity<GlobalResponse<String>> createNewsVector(){
        newsService.collectAll();
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(201, "success"));
    }

    @PostMapping("/news/market/fetch")
    @Operation(summary = "시장 뉴스 수동 수집", description = "코스피, 금리, 달러 등 시장 전반 뉴스를 수동으로 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "수집 성공"),
            @ApiResponse(responseCode = "500", description = "수집 실패")
    })
    public ResponseEntity<GlobalResponse<String>> fetchMarketNews() {
        marketNewsCollectService.collectMarketNews();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(201, "SUCCESS"));
    }

    @PostMapping("/news/ticker")
    @Operation(summary = "뉴스 직접 등록", description = "ticker와 회사명 검증 후 뉴스를 직접 등록하고 벡터 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "ticker와 회사명 불일치"),
            @ApiResponse(responseCode = "404", description = "등록되지 않은 ticker"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 뉴스")
    })
    public ResponseEntity<GlobalResponse<NewsCreateResDto>> createNews(
            @RequestBody @Valid NewsCreateReqDto newsCreateReqDto
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(201,newsService.createNews(newsCreateReqDto)));
    }
}
