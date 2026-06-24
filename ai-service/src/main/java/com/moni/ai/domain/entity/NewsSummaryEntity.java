package com.moni.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name="p_news_summary")
public class NewsSummaryEntity extends IdAudit {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false,unique = true)
    private NewsEntity news;

    @Column(name="ticker", length = 10)
    private String ticker;

    @Column(name="summary", columnDefinition = "TEXT", nullable = true)
    private String summary;

    @Column(name="expired_at")
    private LocalDateTime expiredAt;

}
