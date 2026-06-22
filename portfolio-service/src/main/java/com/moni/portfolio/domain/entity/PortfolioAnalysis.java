package com.moni.portfolio.domain.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "p_portfolio_analysis",
        indexes = {
                @Index(
                        name = "idx_analysis_portfolio_analyzedat",
                        columnList = "portfolio_id, analyzed_at"
                )
        }
)
public class PortfolioAnalysis extends BaseEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "portfolio_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_portfolio_analysis_portfolio")
    )
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AnalysisStatus status;

    @Column(name = "total_return_rate", precision = 7, scale = 4)
    private BigDecimal totalReturnRate;

    @Column(name = "total_evaluation_amount", precision = 18, scale = 2)
    private BigDecimal totalEvaluationAmount;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "concentration_score", precision = 5, scale = 2)
    private BigDecimal concentrationScore;

    @Column(name = "concentration_threshold", precision = 5, scale = 2)
    private BigDecimal concentrationThreshold;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Builder
    private PortfolioAnalysis(
            Portfolio portfolio,
            AnalysisStatus status,
            BigDecimal totalReturnRate,
            BigDecimal totalEvaluationAmount,
            String summary,
            BigDecimal concentrationScore,
            BigDecimal concentrationThreshold,
            String errorMessage,
            LocalDateTime analyzedAt
    ) {
        this.portfolio = portfolio;
        this.status = status == null ? AnalysisStatus.PENDING : status;
        this.totalReturnRate = totalReturnRate;
        this.totalEvaluationAmount = totalEvaluationAmount;
        this.summary = summary;
        this.concentrationScore = concentrationScore;
        this.concentrationThreshold = concentrationThreshold;
        this.errorMessage = errorMessage;
        this.analyzedAt = analyzedAt == null ? LocalDateTime.now() : analyzedAt;
    }

    /** 분석 요청 시 사용 */
    public static PortfolioAnalysis request(
            Portfolio portfolio,
            BigDecimal totalReturnRate,
            BigDecimal totalEvaluationAmount
    ) {
        return PortfolioAnalysis.builder()
                .portfolio(portfolio)
                .totalReturnRate(totalReturnRate)
                .totalEvaluationAmount(totalEvaluationAmount)
                .build();
    }

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UuidV7.generate();
        }
    }

    /** 분석 성공 시 사용 */
    public void succeed(
            String summary,
            BigDecimal concentrationScore,
            BigDecimal concentrationThreshold
    ) {
        this.status = AnalysisStatus.SUCCESS;
        this.summary = summary;
        this.concentrationScore = concentrationScore;
        this.concentrationThreshold = concentrationThreshold;
        this.errorMessage = null;
        this.analyzedAt = LocalDateTime.now();
    }

    /** 분석 실패 시 사용 */
    public void fail(String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
        this.analyzedAt = LocalDateTime.now();
    }
}
