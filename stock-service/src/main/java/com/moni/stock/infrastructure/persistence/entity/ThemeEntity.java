package com.moni.stock.infrastructure.persistence.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.stock.domain.entity.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_theme")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThemeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String themeCode;

    @Column(nullable = false, length = 100)
    private String themeName;


    @Builder
    private ThemeEntity(String themeCode, String themeName) {
        this.themeCode = themeCode;
        this.themeName = themeName;
    }

}
