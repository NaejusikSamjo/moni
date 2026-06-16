package com.moni.user.user.domain.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "watchlist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Watchlist extends BaseEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Transient
    private boolean isNew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stock_code", length = 20, nullable = false)
    private String stockCode;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public static Watchlist create(User user, String stockCode) {
        Watchlist watchlist = new Watchlist();
        watchlist.id = UUID.randomUUID();
        watchlist.isNew = true;
        watchlist.user = user;
        watchlist.stockCode = stockCode;
        return watchlist;
    }
}