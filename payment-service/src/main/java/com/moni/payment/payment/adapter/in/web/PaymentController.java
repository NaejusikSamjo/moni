package com.moni.payment.payment.adapter.in.web;

import com.moni.payment.common.response.ApiResponse;
import com.moni.payment.payment.adapter.in.web.dto.SubscribeRequest;
import com.moni.payment.payment.adapter.in.web.dto.SubscribeResponse;
import com.moni.payment.payment.application.command.SubscribeCommand;
import com.moni.payment.payment.application.command.SubscribeResult;
import com.moni.payment.payment.domain.port.in.InitiatePaymentUseCase;
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

    private final InitiatePaymentUseCase initiatePaymentUseCase;

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
        SubscribeResult result = initiatePaymentUseCase.initiatePayment(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, SubscribeResponse.from(result)));
    }
}
