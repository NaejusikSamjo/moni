package com.moni.portfolio.domain.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.portfolio.domain.support.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "p_portfolio",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_portfolio_user_id", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_portfolio_user", columnList = "user_id")
        }
)
public class Portfolio extends BaseEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "ai_analysis_count", nullable = false)
    private Long aiAnalysisCount;

    @Builder
    private Portfolio(UUID userId) {
        this.userId = userId;
        this.aiAnalysisCount = 0L;
    }

    public static Portfolio create(UUID userId) {
        return Portfolio.builder()
                .userId(userId)
                .build();
    }

    @PrePersist
    private void generateId() {
        if (id == null) {
            id = UuidV7.generate();
        }
    }

    /** AI 포트폴리오 분석 횟수 증가 메서드 */
    public void increaseAiAnalysisCount() {
        this.aiAnalysisCount++;
    }
}
