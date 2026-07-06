package com.moni.ai.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import com.moni.ai.domain.enums.SentimentEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyIssueResDto {

    @JsonProperty("ticker")
    public String ticker;

    @JsonProperty("company_name")
    public String companyName;

    @JsonProperty("summary")
    public String summary;

    @JsonProperty("sentiment")
    public SentimentEnum sentiment;

    @JsonProperty("analyzedAt")
    public LocalDateTime analyzedAt;

    @JsonProperty("expiredAt")
    private LocalDateTime expiredAt;

    public static CompanyIssueResDto toDto(CompanyIssueAnalysisEntity entity){
        return CompanyIssueResDto.builder()
                .ticker(entity.getTicker())
                .companyName(entity.getCompanyName())
                .summary(entity.getSummary())
                .sentiment(entity.getSentiment())
                .analyzedAt(entity.getCreatedAt())
                .expiredAt(entity.getExpiredAt())
                .build();
    }
}
