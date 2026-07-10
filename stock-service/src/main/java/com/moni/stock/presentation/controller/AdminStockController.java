package com.moni.stock.presentation.controller;

import com.moni.stock.application.masterFile.StockMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin_Stock", description = "관리자 초기 DB 생성")
@RestController
@RequestMapping("/api/v1/admin/stocks")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockMasterService stockMasterService;

    @Operation(summary = "초기 코스피, 코스닥, 테마 마스터 파일 전처리, 저장 로직", description = "관리자만 호출가능, 초기 한번만 실행하면 되는 로직임.")
    @PostMapping("/download/stocks")
    public Map<String, String> downloadStocks() {

        stockMasterService.runOnceOnStartupKosdaq();
        stockMasterService.runOnceOnStartupKospi();
        stockMasterService.updateThemeMasters();

        return Map.of("message", "success download stocks");
    }
}
