package com.moni.stock.infrastructure.persistence.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.stock.domain.entity.Theme;
import jakarta.persistence.*;
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

    @OneToMany(mappedBy = "theme", fetch = FetchType.LAZY)
    private List<StockThemeEntity> stockThemes;

    @Builder
    private ThemeEntity(String themeCode, String themeName) {
        this.themeCode = themeCode;
        this.themeName = themeName;
    }

    public Theme toDomain() {
        List<String> stockNames = stockThemes == null ? List.of() :
                stockThemes.stream()
                        .map(st -> st.getStock().getName())
                        .toList();
        return Theme.builder()
                .id(id)
                .themeCode(themeCode)
                .themeName(themeName)
                .stockNames(stockNames)
                .build();
    }
}