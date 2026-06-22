package com.moni.stock.presentation.controller;

import com.moni.common.response.paging.PageRes;
import com.moni.stock.application.masterFile.StockMasterService;
import com.moni.stock.application.port.in.StockQueryUseCase;
import com.moni.stock.domain.type.ChartIndex;
import com.moni.stock.presentation.dto.response.StockChartResponse;
import com.moni.stock.presentation.dto.response.StockResDto;
import com.moni.stock.presentation.dto.response.ThemeRankingResponse;
import com.moni.stock.presentation.dto.response.TopVolumeResponse;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Stock", description = "시세 조회")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockQueryUseCase stockQueryUseCase;
    private final StockMasterService stockMasterService;

    @Operation(summary = "초기 코스피, 코스닥, 테마 마스터 파일 전처리, 저장 로직", description = "초기 한번만 실행하면 되는 로직임.")
    @PostMapping("/download/stocks")
    public Map<String, String> downloadStocks() {
        stockMasterService.runOnceOnStartupKosdaq();
        stockMasterService.runOnceOnStartupKospi();
        stockMasterService.updateThemeMasters();

        return Map.of("message", "success download stocks");
    }

    @Operation(summary = "주식 조회 기능 (검색)", description = "주식 조회 기능을 제공한다, 검색어는 필수가 아니다, 페이징처리가 된다.")
    @GetMapping("/search")
    public PageRes<StockResDto> getStockList(@Parameter(description = "회사명", example = "삼성전자") @RequestParam(value = "keyword", required = false) String keyword, @PageableDefault(size = 10, page = 0, sort = "ticker") Pageable pageable) {

        return stockQueryUseCase.getStockList(keyword, pageable);

    }

    @Operation(summary = "주식 상세 조회 기능", description = "주식 단일 항목에 대해 조회 기능 제공")
    @GetMapping("/{ticker}")
    public StockResDto getStockDetail(@Parameter(description = "조회할 주식 Code", example = "000020") @PathVariable String ticker) {
        return stockQueryUseCase.getStockDetail(ticker);
    }

    @Operation(summary = "주식 분봉 조회", description = "단일 주식 항목에 대한 분봉을 제공 (1,3,5,10분 단위 지원)")
    @GetMapping("/{ticker}/chart")
    public StockChartResponse getStockCandle(@Parameter(description = "조회할 주식 Code", example = "000020") @PathVariable String ticker, @Parameter(description = "분봉 시간 단위", example = "MIN_1") @RequestParam("index") ChartIndex index) {
        return stockQueryUseCase.getChart(ticker, index);
    }

    @Operation(summary = "시장 카테고리별 거래량 내림차순 5개 조회 기능", description = "kis api호출 제한을 피하기 위해 스케줄러로 주기적인 조회 및 저장")
    @GetMapping("/themes")
    public List<ThemeRankingResponse> getThemes() {
        return stockQueryUseCase.getThemes();
    }

    @Operation(summary = "단일 종목 거래량 내림차순 5개 조회 기능", description = "거래량이 가장 많은 5개 항목에 대해 조회 기능 제공")
    @GetMapping("/top-volume")
    public TopVolumeResponse getTopVolume() {
        return stockQueryUseCase.getTopVolume();
    }

}