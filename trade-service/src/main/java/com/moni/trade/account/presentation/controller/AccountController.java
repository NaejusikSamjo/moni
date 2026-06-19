package com.moni.trade.account.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.trade.account.application.service.AccountService;
import com.moni.trade.account.presentation.dto.request.AccountCreateRequestDto;
import com.moni.trade.account.presentation.dto.response.AccountResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<GlobalResponse<AccountResponseDto>> findAccount(
            @RequestHeader("X-User-Id") UUID userId) {
        AccountResponseDto response = accountService.findAccountByUserId(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<AccountResponseDto>> createAccount(
            @Valid @RequestBody AccountCreateRequestDto request) {
        AccountResponseDto response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }
}
