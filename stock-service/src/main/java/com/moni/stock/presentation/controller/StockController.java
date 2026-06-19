package com.moni.stock.presentation.controller;

import com.moni.common.response.paging.PageRes;
import com.moni.stock.application.masterFile.StockMasterService;
import com.moni.stock.application.port.in.StockQueryUseCase;
import com.moni.stock.presentation.dto.response.StockResDto;
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

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockQueryUseCase stockQueryUseCase;
    private final StockMasterService stockMasterService;
    
    @PostMapping("/download/stocks")
    public Map<String, String> downloadStocks() {
        stockMasterService.runOnceOnStartupKosdaq();
        stockMasterService.runOnceOnStartupKospi();

        return Map.of("message", "success download stocks");
    }

    @GetMapping("/search")
    public PageRes<StockResDto> getStockList(@RequestParam(value = "keyword", required = false) String keyword, @PageableDefault(size = 10, page = 0, sort = "ticker") Pageable pageable) {

        return stockQueryUseCase.getStockList(keyword, pageable);

    }

    @GetMapping("/{ticker}")
    public StockResDto getStockDetail(@PathVariable String ticker) {
        return stockQueryUseCase.getStockDetail(ticker);
    }

}