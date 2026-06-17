package com.moni.portfolio.domain.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "portfolio_sector_analysis",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sector_analysis_analysis_sector",
                        columnNames = {"analysis_id", "sector_name"}
                )
        },
        indexes = {
                @Index(name = "idx_sector_analysis", columnList = "analysis_id")
        }
)
public class PortfolioSectorAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "analysis_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sector_analysis_analysis")
    )
    private PortfolioAnalysis analysis;

    @Column(name = "sector_name", length = 50, nullable = false)
    private String sectorName;

    @Column(name = "weight", precision = 5, scale = 2, nullable = false)
    private BigDecimal weight;

    @Column(name = "evaluation_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal evaluationAmount;

    @Builder
    private PortfolioSectorAnalysis(
            PortfolioAnalysis analysis,
            String sectorName,
            BigDecimal weight,
            BigDecimal evaluationAmount
    ) {
        this.analysis = analysis;
        this.sectorName = sectorName;
        this.weight = weight;
        this.evaluationAmount = evaluationAmount;
    }

    public static PortfolioSectorAnalysis create(
            PortfolioAnalysis analysis,
            String sectorName,
            BigDecimal weight,
            BigDecimal evaluationAmount
    ) {
        return PortfolioSectorAnalysis.builder()
                .analysis(analysis)
                .sectorName(sectorName)
                .weight(weight)
                .evaluationAmount(evaluationAmount)
                .build();
    }

    /** 비중 및 평가 금액 업데이트 메서드 */
    public void updateAnalysisResult(BigDecimal weight, BigDecimal evaluationAmount) {
        this.weight = weight;
        this.evaluationAmount = evaluationAmount;
    }
}
