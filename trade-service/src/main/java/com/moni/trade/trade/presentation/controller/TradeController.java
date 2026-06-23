package com.moni.trade.trade.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.common.response.paging.PageRes;
import com.moni.trade.trade.application.service.TradeService;
import com.moni.trade.trade.presentation.dto.request.TradeBuyRequestDto;
import com.moni.trade.trade.presentation.dto.request.TradeSellRequestDto;
import com.moni.trade.trade.presentation.dto.response.TradeResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/buy")
    public ResponseEntity<GlobalResponse<TradeResponseDto>> buyStock(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody TradeBuyRequestDto request) {
        TradeResponseDto response = tradeService.buyStock(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }

    @PostMapping("/sell")
    public ResponseEntity<GlobalResponse<TradeResponseDto>> sellStock(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody TradeSellRequestDto request) {
        TradeResponseDto response = tradeService.sellStock(userId, request);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<PageRes<TradeResponseDto>>> findTrades(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageRes<TradeResponseDto> response = tradeService.findTrades(userId, pageable);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
