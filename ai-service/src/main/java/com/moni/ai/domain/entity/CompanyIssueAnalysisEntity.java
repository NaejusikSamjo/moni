package com.moni.ai.domain.entity;

import com.moni.ai.domain.enums.SentimentEnum;
import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="p_company_issue_analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CompanyIssueAnalysisEntity extends IdAudit {

    @Column(name="ticker")
    private String ticker;

    @Column(name = "company_name", length = 50)
    private String companyName;

    @Column(name="summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name="sentiment")
    private SentimentEnum sentiment;

    @Column(name="expired_at")
    private LocalDateTime expiredAt;

    @Builder
    public CompanyIssueAnalysisEntity(
            String ticker,
            String companyName,
            String summary,
            SentimentEnum sentiment,
            LocalDateTime expiredAt
    ){
        this.ticker=ticker;
        this.companyName=companyName;
        this.summary=summary;
        this.sentiment=sentiment;
        this.expiredAt=expiredAt;
    }
}
