package com.moni.user.user.domain.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.user.user.domain.enums.TendencyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "p_tendency")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tendency extends BaseEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Transient
    private boolean isNew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score", nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private TendencyType type;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public static Tendency create(User user, int score) {
        Tendency tendency = new Tendency();
        tendency.id = UUID.randomUUID();
        tendency.isNew = true;
        tendency.user = user;
        tendency.score = score;
        tendency.type = TendencyType.fromScore(score);
        return tendency;
    }

    public void update(int score) {
        this.score = score;
        this.type = TendencyType.fromScore(score);
    }
}