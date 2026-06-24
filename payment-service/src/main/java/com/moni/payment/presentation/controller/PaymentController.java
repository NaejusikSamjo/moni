package com.moni.payment.presentation.controller;

import com.moni.common.response.paging.PageRes;
import com.moni.payment.application.dto.commandDto.SubscribeCommand;
import com.moni.payment.application.dto.commandDto.SubscribeResult;
import com.moni.payment.application.service.PaymentQueryService;
import com.moni.payment.application.service.SubscribePaymentUseCase;
import com.moni.payment.common.response.ApiResponse;
import com.moni.payment.presentation.dto.PaymentHistoryResponse;
import com.moni.payment.presentation.dto.SubscribeRequest;
import com.moni.payment.presentation.dto.SubscribeResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final SubscribePaymentUseCase subscribePaymentUseCase;
    private final PaymentQueryService paymentQueryService;

    @Operation(summary = "toss PG사 정기 구독 API",
    description = "클라이언트가 필수입니다. 또한 현재 서비스 가격 등을 클라이언트 측에 받고 있는 형태입니다.")
    @PostMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscribeResponse>> subscribe(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SubscribeRequest request) {
        SubscribeCommand command = new SubscribeCommand(
                userId,
                request.authKey(),
                request.customerKey(),
                request.amount(),
                userId.toString());
        SubscribeResult result = subscribePaymentUseCase.execute(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, SubscribeResponse.from(result)));
    }

    @Operation(summary = "결제 내역 조회 API", description = "사용자의 결제 내역을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<PaymentHistoryResponse>>> getPayments(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        PageRes<PaymentHistoryResponse> result = paymentQueryService.execute(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
