package com.moni.payment.presentation.controller;

import com.moni.payment.common.response.ApiResponse;
import com.moni.payment.presentation.dto.SubscribeRequest;
import com.moni.payment.presentation.dto.SubscribeResponse;
import com.moni.payment.application.dto.commandDto.SubscribeCommand;
import com.moni.payment.application.dto.commandDto.SubscribeResult;
import com.moni.payment.application.service.SubscribePaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
