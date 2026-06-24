package com.moni.trade.holding.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.common.response.paging.PageRes;
import com.moni.trade.holding.application.service.HoldingService;
import com.moni.trade.holding.presentation.dto.response.HoldingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;

    @GetMapping
    public ResponseEntity<GlobalResponse<PageRes<HoldingResponseDto>>> findHoldings(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageRes<HoldingResponseDto> response = holdingService.findHoldings(userId, pageable);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<GlobalResponse<HoldingResponseDto>> findHolding(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String ticker) {
        HoldingResponseDto response = holdingService.findHolding(userId, ticker);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
