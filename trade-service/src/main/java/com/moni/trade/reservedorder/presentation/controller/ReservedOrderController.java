package com.moni.trade.reservedorder.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.trade.reservedorder.application.service.ReservedOrderService;
import com.moni.trade.reservedorder.presentation.dto.request.ReservedBuyOrderRequestDto;
import com.moni.trade.reservedorder.presentation.dto.request.ReservedSellOrderRequestDto;
import com.moni.trade.reservedorder.presentation.dto.response.ReservedOrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reserved-orders")
@RequiredArgsConstructor
public class ReservedOrderController {

    private final ReservedOrderService reservedOrderService;

    @PostMapping("/buy")
    public ResponseEntity<GlobalResponse<ReservedOrderResponseDto>> createBuyOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ReservedBuyOrderRequestDto request) {
        ReservedOrderResponseDto response = reservedOrderService.createBuyOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }

    @PostMapping("/sell")
    public ResponseEntity<GlobalResponse<ReservedOrderResponseDto>> createSellOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ReservedSellOrderRequestDto request) {
        ReservedOrderResponseDto response = reservedOrderService.createSellOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<GlobalResponse<Void>> cancelOrder(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID orderId) {
        reservedOrderService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), null));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<List<ReservedOrderResponseDto>>> findMyOrders(
            @RequestHeader("X-User-Id") UUID userId) {
        List<ReservedOrderResponseDto> response = reservedOrderService.findMyOrders(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
