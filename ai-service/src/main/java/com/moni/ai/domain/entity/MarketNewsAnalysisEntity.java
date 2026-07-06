package com.moni.ai.domain.entity;

import io.micrometer.core.instrument.Meter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketNewsAnalysisEntity extends IdAudit {

    @Column(name="keyword",length = 50)
    private String keyword;

    @Column(name="summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name="expired_at")
    private LocalDateTime expiredAt;

    @Builder
    public MarketNewsAnalysisEntity(
            String keyword,
            String summary,
            LocalDateTime expiredAt
    ){
        this.keyword=keyword;
        this.summary=summary;
        this.expiredAt=expiredAt;
    }
}
