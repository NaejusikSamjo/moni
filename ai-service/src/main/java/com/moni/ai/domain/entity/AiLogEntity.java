package com.moni.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="p_ai_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor
@Builder
public class AiLogEntity extends IdAudit{

    @Column(name="prompt",columnDefinition = "TEXT")
    private String prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_analysis_id")
    private CompanyIssueAnalysisEntity companyAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="market_news_analysis_id")
    private MarketNewsAnalysisEntity marketAnalysis;
}
