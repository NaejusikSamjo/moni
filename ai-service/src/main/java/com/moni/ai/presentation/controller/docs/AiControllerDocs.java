package com.moni.ai.presentation.controller.docs;

import com.moni.ai.presentation.dto.request.IssueAnalysisReqDto;
import com.moni.ai.presentation.dto.response.CompanyIssueResDto;
import com.moni.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "AI", description = "AI 분석 API")
public interface AiControllerDocs {

    @Operation(summary = "기업 이슈 분석 생성", description = "RAG 기반으로 기업 이슈를 분석합니다. 캐시가 없을 경우 LLM을 호출합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 성공"),
            @ApiResponse(responseCode = "404", description = "등록되지 않은 ticker"),
            @ApiResponse(responseCode = "500", description = "LLM 호출 실패")
    })
    ResponseEntity<GlobalResponse<CompanyIssueResDto>> createIssueAnalysis(
            @Parameter(description = "종목코드 (예: 005930)", example = "005930") String ticker,
            IssueAnalysisReqDto request
    );

    @Operation(summary = "기업 이슈 분석 조회", description = "캐시된 분석 결과를 조회합니다. LLM 호출 없이 저장된 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "분석 결과 없음 또는 등록되지 않은 ticker")
    })
    ResponseEntity<GlobalResponse<CompanyIssueResDto>> getIssueAnalysis(
            @Parameter(description = "종목코드 (예: 005930)", example = "005930") String ticker
    );

    @Operation(summary = "뉴스 수동 수집", description = "등록된 전체 기업의 뉴스를 수동으로 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "수집 성공"),
            @ApiResponse(responseCode = "500", description = "수집 실패")
    })
    ResponseEntity<GlobalResponse<String>> createNewsVector();
}
