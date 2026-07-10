package com.moni.trade.asset.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.trade.asset.application.service.AssetService;
import com.moni.trade.asset.presentation.dto.response.AssetAnalysisSnapshotResponseDto;
import com.moni.trade.asset.presentation.dto.response.AssetHoldingsResponseDto;
import com.moni.trade.asset.presentation.dto.response.AssetResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Asset", description = "자산 및 보유 종목 API")
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String DEFAULT_SORT = "evaluationAmount,desc";

    private final AssetService assetService;

    @Operation(summary = "자산 조회", description = "예수금과 보유 종목 평가 결과를 합산한 자산 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse<AssetResponseDto>> getAssets(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        AssetResponseDto response = assetService.getAssets(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "보유 종목 현황 조회", description = "보유 종목별 평가금액, 손익, 수익률과 포트폴리오 비중을 페이지로 조회합니다.")
    @GetMapping("/holdings")
    public ResponseEntity<GlobalResponse<AssetHoldingsResponseDto>> getHoldings(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId,
            @Parameter(description = "페이지 번호이며 음수는 0으로 보정됩니다.", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기이며 1부터 50 이외의 값은 10으로 보정됩니다.", schema = @Schema(minimum = "1", maximum = "50"))
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "평가금액 정렬 조건", schema = @Schema(allowableValues = {"evaluationAmount,desc", "evaluationAmount,asc"}))
            @RequestParam(defaultValue = DEFAULT_SORT) String sort
    ) {
        AssetHoldingsResponseDto response = assetService.getHoldings(userId, page, size, sort);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "AI 분석용 자산 스냅샷 조회", description = "AI 포트폴리오 분석에 필요한 자산 요약과 보유 비중 상위 종목을 한 번에 조회합니다.")
    @GetMapping("/analysis-snapshot")
    public ResponseEntity<GlobalResponse<AssetAnalysisSnapshotResponseDto>> getAnalysisSnapshot(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        AssetAnalysisSnapshotResponseDto response = assetService.getAnalysisSnapshot(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
