package com.moni.trade.account.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.trade.account.application.service.AccountService;
import com.moni.trade.account.presentation.dto.response.AccountResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
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
            @RequestHeader("X-User-Id") UUID userId) {
        AccountResponseDto response = accountService.createAccount(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }
}
